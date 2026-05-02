package com.batu.transaction_service.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.TransactionDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;
import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.entity.TransactionDetailedCategory;
import com.batu.transaction_service.exception.ResourceNotFoundException;
import com.batu.transaction_service.mapper.TransactionSyncMapper;
import com.batu.transaction_service.repository.TransactionRepository;
import com.batu.transaction_service.repository.spec.TransactionSpecs;
import com.batu.transaction_service.service.DetailedCategoryService;
import com.batu.transaction_service.service.TransactionService;
import com.batu.transaction_service.service.input.RecordTransactionInput;
import com.batu.transaction_service.util.CursorUtils;

@Service
public class TransactionServiceImpl implements TransactionService {

        private final TransactionRepository transactionRepository;
        private final CursorUtils cursorUtils;
        private final DetailedCategoryService detailedCategoryService;
        private final TransactionSyncMapper transactionSyncMapper;

        public TransactionServiceImpl(TransactionRepository transactionRepository, CursorUtils cursorUtils,
                        DetailedCategoryService detailedCategoryService,
                        TransactionSyncMapper transactionSyncMapper) {
                this.transactionRepository = transactionRepository;
                this.cursorUtils = cursorUtils;
                this.detailedCategoryService = detailedCategoryService;
                this.transactionSyncMapper = transactionSyncMapper;
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
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Transaction with id " + transactionId + " not found"));

                return transactionSyncMapper.toDto(transaction);
        }

        @Override
        @Transactional
        public Optional<Transaction> recordTransaction(RecordTransactionInput input) {
                Transaction transaction = transactionRepository.findById(input.transactionId()).orElse(null);

                if (transaction != null && input.version() <= transaction.getSyncVersion()) {
                        return Optional.empty();
                }

                if (transaction == null && !input.active()) {
                        return Optional.empty();
                }

                TransactionDetailedCategory detailedCategory = detailedCategoryService
                                .getByCategoryCode(input.detailedCategoryCode());

                if (transaction == null) {
                        transaction = new Transaction(
                                        input.transactionId(),
                                        input.userId(),
                                        input.accountId(),
                                        input.amount(),
                                        input.isoCurrencyCode(),
                                        input.transactionName(),
                                        input.transactionType(),
                                        input.date(),
                                        input.pending(),
                                        input.paymentChannel(),
                                        detailedCategory,
                                        input.active(),
                                        input.version());
                } else {
                        transaction.setUserId(input.userId());
                        transaction.setAccountId(input.accountId());
                        transaction.setAmount(input.amount());
                        transaction.setIsoCurrencyCode(input.isoCurrencyCode());
                        transaction.setTransactionName(input.transactionName());
                        transaction.setTransactionType(input.transactionType());
                        transaction.setDate(input.date());
                        transaction.setPending(input.pending());
                        transaction.setPaymentChannel(input.paymentChannel());
                        transaction.setDetailedCategory(detailedCategory);
                        transaction.setActive(input.active());
                        transaction.setSyncVersion(input.version());
                }

                return Optional.of(transactionRepository.save(transaction));
        }

        @Override
        @Transactional
        public List<Transaction> deactivateByAccountId(UUID accountId, long version) {
                List<Transaction> transactions = transactionRepository.findByAccountIdAndIsActiveTrue(accountId)
                                .stream()
                                .filter(transaction -> version > transaction.getSyncVersion())
                                .toList();

                for (Transaction transaction : transactions) {
                        transaction.setActive(false);
                        transaction.setSyncVersion(version);
                }

                return transactionRepository.saveAll(transactions);
        }

}
