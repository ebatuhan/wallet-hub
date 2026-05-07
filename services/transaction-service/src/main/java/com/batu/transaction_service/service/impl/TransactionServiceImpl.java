package com.batu.transaction_service.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.batu.shared.dto.request.TransactionUpsertRequestDto;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.TransactionDto;
import com.batu.shared.dto.response.TransactionUpsertResponseDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;
import com.batu.shared.messaging.event.TransactionRecorded;
import com.batu.shared.messaging.event.TransactionRemoved;
import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.entity.TransactionDetailedCategory;
import com.batu.transaction_service.mapper.TransactionSyncMapper;
import com.batu.transaction_service.messaging.OutboxDomainEventPublisher;
import com.batu.transaction_service.repository.TransactionRepository;
import com.batu.transaction_service.repository.spec.TransactionSpecs;
import com.batu.transaction_service.service.DetailedCategoryService;
import com.batu.transaction_service.service.TransactionService;
import com.batu.shared.cursor.CursorUtils;

@Service
public class TransactionServiceImpl implements TransactionService {

        private final TransactionRepository transactionRepository;
        private final CursorUtils cursorUtils;
        private final DetailedCategoryService detailedCategoryService;
        private final TransactionSyncMapper transactionSyncMapper;
        private final OutboxDomainEventPublisher eventPublisher;

        public TransactionServiceImpl(TransactionRepository transactionRepository, CursorUtils cursorUtils,
                        DetailedCategoryService detailedCategoryService,
                        TransactionSyncMapper transactionSyncMapper,
                        OutboxDomainEventPublisher eventPublisher) {
                this.transactionRepository = transactionRepository;
                this.cursorUtils = cursorUtils;
                this.detailedCategoryService = detailedCategoryService;
                this.transactionSyncMapper = transactionSyncMapper;
                this.eventPublisher = eventPublisher;
        }

        @Override
        @Transactional(readOnly = true)
        public CursorResponse<TransactionViewResponseDto> transactions(Jwt principal, String category, UUID accountId,
                        String cursor,
                        int limit) {
                return transactions(UUID.fromString(principal.getSubject()), category, accountId, cursor, limit);
        }

        @Override
        @Transactional(readOnly = true)
        public CursorResponse<TransactionViewResponseDto> transactions(UUID userId, String category, UUID accountId,
                        String cursor,
                        int limit) {
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

                var dtos = window.getContent().stream()
                                .map(
                                                tx -> new TransactionViewResponseDto(tx.getTransactionId(),
                                                                tx.getAmount(),
                                                                tx.getTransactionName(),
                                                                tx.getTransactionType(),
                                                                tx.getDate(),
                                                                tx.getPending(),
                                                                tx.getPaymentChannel(),
                                                                tx.getIsoCurrencyCode(),
                                                                tx.getDetailedCategory().getTransactionPrimaryCategory()
                                                                                .getTransactionPrimaryCategoryId(),
                                                                tx.getDetailedCategory().getTransactionPrimaryCategory()
                                                                                .getCategoryCode(),
                                                                tx.getDetailedCategory().getTransactionPrimaryCategory()
                                                                                .getDisplayName(),
                                                                tx.getDetailedCategory().getTransactionPrimaryCategory()
                                                                                .getIconUrl(),
                                                                tx.getDetailedCategory().getTransactionDetailedCategoryId(),
                                                                tx.getDetailedCategory().getCategoryCode(),
                                                                tx.getDetailedCategory().getDisplayName(),
                                                                tx.getAccountId()))
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
                return getTransactionById(UUID.fromString(principal.getSubject()), transactionId);
        }

        @Override
        public TransactionDto getTransactionById(UUID userId, UUID transactionId) {
                Transaction transaction = transactionRepository
                                .findByTransactionIdAndUserIdWithCategory(transactionId, userId)
                                .filter(Transaction::isActive)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Transaction with id " + transactionId + " not found"));

                return transactionSyncMapper.toDto(transaction);
        }

        @Override
        @Transactional
        public TransactionUpsertResponseDto upsertTransaction(TransactionUpsertRequestDto request) {
                TransactionDetailedCategory detailedCategory = detailedCategoryService
                                .getByCategoryCode(request.getDetailedCategoryCode());
                Transaction transaction = transactionRepository.upsertTransaction(
                                request,
                                detailedCategory.getTransactionDetailedCategoryId());
                if (transaction.isActive()) {
                        eventPublisher.publishTransactionRecorded(new TransactionRecorded(
                                        transaction.getTransactionId(),
                                        transaction.getUserId(),
                                        transaction.getAccountId(),
                                        transaction.getAmount(),
                                        transaction.getIsoCurrencyCode(),
                                        transaction.getTransactionName(),
                                        transaction.getTransactionType(),
                                        transaction.getDate(),
                                        transaction.getPending(),
                                        transaction.getPaymentChannel(),
                                        detailedCategory.getTransactionPrimaryCategory().getTransactionPrimaryCategoryId(),
                                        detailedCategory.getTransactionPrimaryCategory().getCategoryCode(),
                                        true));
                } else {
                        eventPublisher.publishTransactionRemoved(new TransactionRemoved(
                                        transaction.getTransactionId(),
                                        transaction.getUserId(),
                                        transaction.getAccountId()));
                }

                return toUpsertResponse(transaction, detailedCategory);
        }

        @Override
        @Transactional
        public List<TransactionUpsertResponseDto> deactivateTransactionsByAccountId(UUID accountId) {
                List<Transaction> transactions = transactionRepository.findByAccountIdAndIsActiveTrue(accountId);

                for (Transaction transaction : transactions) {
                        transaction.setActive(false);
                }

                List<Transaction> savedTransactions = transactionRepository.saveAll(transactions);

                for (Transaction transaction : savedTransactions) {
                        eventPublisher.publishTransactionRemoved(new TransactionRemoved(
                                        transaction.getTransactionId(),
                                        transaction.getUserId(),
                                        transaction.getAccountId()));
                }

                return savedTransactions.stream()
                                .map(transaction -> toUpsertResponse(transaction, transaction.getDetailedCategory()))
                                .toList();
        }

        private TransactionUpsertResponseDto toUpsertResponse(
                        Transaction transaction,
                        TransactionDetailedCategory detailedCategory) {
                return new TransactionUpsertResponseDto(
                                transaction.getTransactionId(),
                                transaction.getUserId(),
                                transaction.getAccountId(),
                                transaction.getAmount(),
                                transaction.getIsoCurrencyCode(),
                                transaction.getTransactionName(),
                                transaction.getTransactionType(),
                                transaction.getDate(),
                                transaction.getPending(),
                                transaction.getPaymentChannel(),
                                detailedCategory.getCategoryCode(),
                                transaction.isActive(),
                                transaction.getCreatedAt(),
                                transaction.getUpdatedAt());
        }

}
