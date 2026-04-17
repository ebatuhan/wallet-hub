package com.batu.transaction_service.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batu.shared.dto.request.AccountIdsRequestDto;
import com.batu.shared.dto.request.AccountNameRequestDto;
import com.batu.shared.dto.request.TransactionRequestDto;
import com.batu.shared.dto.request.TransactionsUpsertRequestDto;
import com.batu.shared.dto.response.AccountNameResponseDto;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.TransactionDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;
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
import com.batu.transaction_service.util.CursorUtils;

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
        public CursorResponse<TransactionViewResponseDto> transatcions(Jwt principal, String category, UUID accountId,
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
        public TransactionDto getTransactionById(Jwt principial, UUID transactionId) {
                UUID userId = UUID.fromString(principial.getSubject());
                Transaction transaction = transactionRepository
                                .findByTransactionIdAndUserIdWithCategory(transactionId, userId)
                                .filter(Transaction::isActive)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transaction with id " + transactionId + " not found"));

                return transactionSyncMapper.toDto(transaction);
        }

        @Override
        @Transactional
        public void saveBatch(TransactionsUpsertRequestDto request) {
                syncTransactions(request.getTransactions());
        }

        @Override
        @Transactional
        public void deactivateByAccountIds(AccountIdsRequestDto request) {
                if (request.getAccountIds().isEmpty()) {
                        return;
                }

                List<Transaction> transactions = transactionRepository.findByAccountIdInAndIsActiveTrue(request.getAccountIds());

                if (transactions.isEmpty()) {
                        return;
                }

                for (Transaction transaction : transactions) {
                        transaction.setActive(false);
                }

                transactionRepository.saveAll(transactions);
                publishPersistedEvents(transactions);
        }

        private void syncTransactions(List<TransactionRequestDto> requests) {
                if (requests.isEmpty()) {
                        return;
                }

                Map<UUID, Transaction> existingTransactionsById = transactionRepository
                                .findAllByTransactionIdInWithCategory(requests.stream()
                                                .map(TransactionRequestDto::getTransactionId)
                                                .toList())
                                .stream()
                                .collect(Collectors.toMap(Transaction::getTransactionId, transaction -> transaction));

                List<Transaction> transactionsToPersist = new ArrayList<>();
                List<Transaction> changedTransactions = new ArrayList<>();

                for (TransactionRequestDto request : requests) {
                        if (!request.isActive()) {
                                Transaction existingTransaction = existingTransactionsById.get(request.getTransactionId());
                                if (existingTransaction == null || !existingTransaction.isActive()) {
                                        continue;
                                }

                                validateUserOwnership(existingTransaction, request.getUserId());
                                existingTransaction.setActive(false);
                                transactionsToPersist.add(existingTransaction);
                                changedTransactions.add(existingTransaction);
                                continue;
                        }

                        TransactionDetailedCategory detailedCategory = detailedCategoryService
                                        .getByCategoryCode(request.getDetailedCategoryCode());

                        Transaction existingTransaction = existingTransactionsById.get(request.getTransactionId());
                        if (existingTransaction == null) {
                                Transaction transaction = transactionSyncMapper.toEntity(request, detailedCategory);
                                transactionsToPersist.add(transaction);
                                changedTransactions.add(transaction);
                                continue;
                        }

                        validateUserOwnership(existingTransaction, request.getUserId());

                        if (!applyTransactionState(existingTransaction, request, detailedCategory)) {
                                continue;
                        }

                        transactionsToPersist.add(existingTransaction);
                        changedTransactions.add(existingTransaction);
                }

                if (transactionsToPersist.isEmpty()) {
                        return;
                }

                transactionRepository.saveAll(transactionsToPersist);
                publishPersistedEvents(changedTransactions);
        }

        private boolean applyTransactionState(Transaction target, TransactionRequestDto request,
                        TransactionDetailedCategory detailedCategory) {
                boolean changed = false;

                changed |= updateIfChanged(target.getAccountId(), request.getAccountId(), target::setAccountId);
                changed |= updateIfChanged(target.getAmount(), request.getAmount(), target::setAmount);
                changed |= updateIfChanged(target.getIsoCurrencyCode(), request.getIsoCurrencyCode(), target::setIsoCurrencyCode);
                changed |= updateIfChanged(target.getTransactionName(), request.getTransactionName(), target::setTransactionName);
                changed |= updateIfChanged(target.getTransactionType(), request.getTransactionType(), target::setTransactionType);
                changed |= updateIfChanged(target.getDate(), request.getDate(), target::setDate);
                changed |= updateIfChanged(target.getPending(), request.getPending(), target::setPending);
                changed |= updateIfChanged(target.getPaymentChannel(), request.getPaymentChannel(), target::setPaymentChannel);
                String currentCategoryCode = target.getDetailedCategory() == null
                                ? null
                                : target.getDetailedCategory().getCategoryCode();
                changed |= updateIfChanged(currentCategoryCode, detailedCategory.getCategoryCode(),
                                value -> target.setDetailedCategory(detailedCategory));
                changed |= updateIfChanged(target.isActive(), request.isActive(), target::setActive);

                return changed;
        }

        private void publishPersistedEvents(List<Transaction> transactions) {
                eventPublisher.publishEvent(new TransactionsPersistedDomainEvent(
                                transactions.stream()
                                                .map(transactionSyncMapper::toPersistedEvent)
                                                .toList()));
        }

        private void validateUserOwnership(Transaction existingTransaction, UUID requestedUserId) {
                if (!existingTransaction.getUserId().equals(requestedUserId)) {
                        throw new IllegalStateException(
                                        "Transaction ownership mismatch for transaction " + existingTransaction.getTransactionId());
                }
        }

        private boolean updateIfChanged(String currentValue, String nextValue, Consumer<String> consumer) {
                if (Objects.equals(currentValue, nextValue)) {
                        return false;
                }

                consumer.accept(nextValue);
                return true;
        }

        private boolean updateIfChanged(UUID currentValue, UUID nextValue, Consumer<UUID> consumer) {
                if (Objects.equals(currentValue, nextValue)) {
                        return false;
                }

                consumer.accept(nextValue);
                return true;
        }

        private boolean updateIfChanged(LocalDate currentValue, LocalDate nextValue, Consumer<LocalDate> consumer) {
                if (Objects.equals(currentValue, nextValue)) {
                        return false;
                }

                consumer.accept(nextValue);
                return true;
        }

        private boolean updateIfChanged(Boolean currentValue, Boolean nextValue, Consumer<Boolean> consumer) {
                if (Objects.equals(currentValue, nextValue)) {
                        return false;
                }

                consumer.accept(nextValue);
                return true;
        }

        private boolean updateIfChanged(BigDecimal currentValue, BigDecimal nextValue, Consumer<BigDecimal> consumer) {
                if (currentValue == null && nextValue == null) {
                        return false;
                }

                if (currentValue != null && nextValue != null && currentValue.compareTo(nextValue) == 0) {
                        return false;
                }

                consumer.accept(nextValue);
                return true;
        }

        private boolean updateIfChanged(boolean currentValue, boolean nextValue, Consumer<Boolean> consumer) {
                if (currentValue == nextValue) {
                        return false;
                }

                consumer.accept(nextValue);
                return true;
        }
}
