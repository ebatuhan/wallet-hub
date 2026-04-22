package com.batu.transaction_service.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batu.shared.dto.request.AccountNameRequestDto;
import com.batu.shared.dto.request.TransactionRequestDto;
import com.batu.shared.dto.response.AccountNameResponseDto;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.TransactionDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;
import com.batu.shared.util.CursorUtils;
import com.batu.transaction_service.client.AccountServiceClient;
import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.entity.TransactionDetailedCategory;
import com.batu.transaction_service.exception.ResourceNotFoundException;
import com.batu.transaction_service.mapper.TransactionSyncMapper;
import com.batu.transaction_service.messaging.TransactionsPersistedDomainEvent;
import com.batu.transaction_service.repository.TransactionRepository;
import com.batu.transaction_service.repository.spec.TransactionSpecs;
import com.batu.transaction_service.service.DetailedCategoryService;
import com.batu.transaction_service.service.TransactionService;

@Service
public class TransactionServiceImpl implements TransactionService {

        private final TransactionRepository transactionRepository;
        private final CursorUtils cursorUtils;
        private final AccountServiceClient accountClient;
        private final DetailedCategoryService detailedCategoryService;
        private final ApplicationEventPublisher eventPublisher;
        private final TransactionSyncMapper transactionSyncMapper;

        public TransactionServiceImpl(TransactionRepository transactionRepository, CursorUtils cursorUtils,
                        AccountServiceClient accountService, DetailedCategoryService detailedCategoryService,
                        ApplicationEventPublisher eventPublisher,
                        TransactionSyncMapper transactionSyncMapper) {
                this.transactionRepository = transactionRepository;
                this.cursorUtils = cursorUtils;
                this.accountClient = accountService;
                this.detailedCategoryService = detailedCategoryService;
                this.eventPublisher = eventPublisher;
                this.transactionSyncMapper = transactionSyncMapper;
        }

        @Override
        @Transactional(readOnly = true)
        public CursorResponse<TransactionViewResponseDto> transactions(Jwt principal, String category, UUID accountId,
                        String cursor,
                        int limit) {

                UUID userId = UUID.fromString(principal.getSubject());

                Specification<Transaction> spec = TransactionSpecs.withDynamicFilters(userId, accountId, category);

                Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("transactionId"));

                ScrollPosition position = (cursor == null || cursor.isEmpty())
                                ? ScrollPosition.keyset()
                                : cursorUtils.decode(cursor);

                Window<Transaction> window = transactionRepository
                                .<Transaction, Window<Transaction>>findBy(spec, query -> query
                                                .sortBy(sort)
                                                .limit(limit)
                                                .scroll(position));

                Set<UUID> accountIds = window.getContent()
                                .stream()
                                .map(acc -> acc.getAccountId())
                                .collect(Collectors.toSet());

                var request = new AccountNameRequestDto(accountIds);
                List<AccountNameResponseDto> response = accountClient
                                .getAccountNames(request)
                                .getBody();

                List<AccountNameResponseDto> accountNames = response == null ? List.of() : response;

                Map<UUID, String> accountInformationsMap = accountNames.stream().collect(Collectors.toMap(
                                AccountNameResponseDto::getAccountId,
                                AccountNameResponseDto::getAccountName));

                var dtos = window.getContent().stream()
                                .map(
                                                tx -> new TransactionViewResponseDto(tx.getTransactionId(),
                                                                tx.getAmount(),
                                                                tx.getTransactionName(),
                                                                tx.getIsoCurrencyCode(),
                                                                tx.getDetailedCategory().getTransactionPrimaryCategory()
                                                                                .getDisplayName(),
                                                                 tx.getDetailedCategory().getDisplayName(),
                                                                 tx.getAccountId(),
                                                                 accountInformationsMap.getOrDefault(tx.getAccountId(),
                                                                                 "")))
                                .collect(Collectors.toList());

                String nextCursor = null;

                if (window.hasNext()) {
                        ScrollPosition nextPos = window.positionAt(window.getContent().size() - 1);
                        nextCursor = cursorUtils.encode(nextPos);
                }

                return new CursorResponse<>(dtos, window.hasNext(), nextCursor);
        }

        @Override
        public TransactionDto getTransactionById(Jwt principal, UUID transactionId) {
                UUID userId = UUID.fromString(principal.getSubject());
                Transaction transaction = transactionRepository
                                .findByTransactionIdAndUserIdWithCategory(transactionId, userId)
                                .filter(Transaction::isActive)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transaction with id " + transactionId + " not found"));

                return transactionSyncMapper.toDto(transaction);
        }

        @Override
        @Transactional
        public void create(TransactionRequestDto request) {
                TransactionDetailedCategory detailedCategory = detailedCategoryService
                                .getByCategoryCode(request.getDetailedCategoryCode());
                Transaction transaction = transactionSyncMapper.toEntity(request, detailedCategory);
                transactionRepository.save(transaction);
                publishPersistedEvents(List.of(transaction));
        }

        @Override
        @Transactional
        public void update(TransactionRequestDto request) {
                Transaction transaction = transactionRepository.findByTransactionIdAndUserIdWithCategory(
                                request.getTransactionId(),
                                request.getUserId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transaction with id " + request.getTransactionId() + " not found"));

                if (!request.isActive()) {
                        transaction.setActive(false);
                        transactionRepository.save(transaction);
                        publishPersistedEvents(List.of(transaction));
                        return;
                }

                TransactionDetailedCategory detailedCategory = detailedCategoryService
                                .getByCategoryCode(request.getDetailedCategoryCode());

                transaction.setAccountId(request.getAccountId());
                transaction.setAmount(request.getAmount());
                transaction.setIsoCurrencyCode(request.getIsoCurrencyCode());
                transaction.setTransactionName(request.getTransactionName());
                transaction.setTransactionType(request.getTransactionType());
                transaction.setDate(request.getDate());
                transaction.setPending(request.getPending());
                transaction.setPaymentChannel(request.getPaymentChannel());
                transaction.setDetailedCategory(detailedCategory);
                transaction.setActive(true);

                transactionRepository.save(transaction);
                publishPersistedEvents(List.of(transaction));
        }

        @Override
        @Transactional
        public void deactivateByAccountId(UUID accountId) {
                List<Transaction> transactions = transactionRepository.findByAccountIdAndIsActiveTrue(accountId);

                if (transactions.isEmpty()) {
                        return;
                }

                for (Transaction transaction : transactions) {
                        transaction.setActive(false);
                }

                transactionRepository.saveAll(transactions);
                publishPersistedEvents(transactions);
        }

        private void publishPersistedEvents(List<Transaction> transactions) {
                eventPublisher.publishEvent(new TransactionsPersistedDomainEvent(
                                transactions.stream()
                                                .map(transactionSyncMapper::toPersistedEvent)
                                                .toList()));
        }

}
