package com.batu.transaction_service.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.batu.transaction_service.dto.TransactionDetailedCategoryDto;
import com.batu.transaction_service.dto.TransactionDto;
import com.batu.transaction_service.dto.TransactionPrimaryCategoryDto;
import com.batu.shared.dto.AccountNameRequestDto;
import com.batu.shared.dto.AccountNameResponseDto;
import com.batu.shared.dto.TransactionsUpsertRequestDto;
import com.batu.transaction_service.client.AccountServiceClient;
import com.batu.transaction_service.dto.CursorResponse;
import com.batu.transaction_service.dto.TransactionViewResponseDto;
import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.repository.TransactionRepository;
import com.batu.transaction_service.repository.spec.TransactionSpecs;
import com.batu.transaction_service.util.CursorUtils;

@Service
public class TransactionServiceImpl {

    private final TransactionRepository transactionRepository;
    private final CursorUtils cursorUtils;
    private final AccountServiceClient accountClient;

    public TransactionServiceImpl(TransactionRepository transactionRepository, CursorUtils cursorUtils,
            AccountServiceClient accountService) {
        this.transactionRepository = transactionRepository;
        this.cursorUtils = cursorUtils;
        this.accountClient = accountService;
    }

    @Transactional(readOnly = true)
    public CursorResponse<TransactionViewResponseDto> transatcions(Jwt principal, String category, String cursor,
            int limit) {

        UUID userId = UUID.fromString(principal.getSubject());

        Specification<Transaction> spec = TransactionSpecs.withDynamicFilters(userId, category);

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

        Map<UUID, String> accountInformationsMap = response.stream().collect(Collectors.toMap(
                AccountNameResponseDto::getAccountId,
                AccountNameResponseDto::getAccountName));

        var dtos = window.getContent().stream()
                .map(
                        tx -> new TransactionViewResponseDto(tx.getTransactionId(),
                                tx.getAmount(),
                                tx.getTransactionName(),
                                tx.getIsoCurrencyCode(),
                                tx.getDetailedCategory().getTransactionPrimaryCategory().getDisplayName(),
                                tx.getDetailedCategory().getDisplayName(),
                                tx.getAccountId(),
                                accountInformationsMap.getOrDefault(tx.getAccountId(), " ")))
                .collect(Collectors.toList());

        String nextCursor = null;

        if (window.hasNext()) {
            ScrollPosition nextPos = window.positionAt(window.getContent().size() - 1);
            nextCursor = cursorUtils.encode(nextPos);
        }

        return new CursorResponse<>(dtos, window.hasNext(), nextCursor);
    }

    public TransactionDto getTransactionById(Jwt principial, UUID transactionId) {
        UUID userId = UUID.fromString(principial.getSubject());
        Transaction transaction = transactionRepository
                .findByTransactionIdAndUserIdAndIsActiveTrue(transactionId, userId)
                .get();

        return mapToResponseDto(transaction);
    }

    private TransactionDto mapToResponseDto(Transaction t) {
        if (t == null) {
            return null;
        }

        TransactionPrimaryCategoryDto primaryCategoryDto = new TransactionPrimaryCategoryDto(
                t.getDetailedCategory().getTransactionPrimaryCategory().getTransactionPrimaryCategoryId(),
                t.getDetailedCategory().getTransactionPrimaryCategory().getCategoryCode(),
                t.getDetailedCategory().getTransactionPrimaryCategory().getDisplayName(),
                t.getDetailedCategory().getTransactionPrimaryCategory().getIconUrl());

        TransactionDetailedCategoryDto detailedCategoryDto = new TransactionDetailedCategoryDto(
                t.getDetailedCategory().getTransactionDetailedCategoryId(),
                t.getDetailedCategory().getDisplayName(),
                t.getDetailedCategory().getDetailedCode(),
                primaryCategoryDto);

        return new TransactionDto(
                t.getTransactionId(),
                t.getUserId(),
                t.getExternalId(),
                t.getAmount(),
                t.getIsoCurrencyCode(),
                t.getTransactionName(),
                t.getTransactionType(),
                t.getDate(),
                t.is_pending(),
                t.getPaymentChannel(),
                detailedCategoryDto,
                t.getCreatedAt(),
                t.getUpdatedAt());
    }

    public Boolean batchUpsertTransactions(TransactionsUpsertRequestDto request){
        return true;
    }
    
}