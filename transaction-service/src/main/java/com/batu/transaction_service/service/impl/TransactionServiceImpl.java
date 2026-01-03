package com.batu.transaction_service.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Limit;
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
import com.batu.transaction_service.dto.CursorResponse;
import com.batu.transaction_service.dto.TransactionPrimaryCategoryDto;
import com.batu.transaction_service.dto.TransactionResponseDto;
import com.batu.transaction_service.dto.TransactionViewResponseDto;
import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.entity.TransactionPrimaryCategory;
import com.batu.transaction_service.repository.TransactionRepository;
import com.batu.transaction_service.repository.spec.TransactionSpecs;
import com.batu.transaction_service.util.CursorUtils;

@Service
public class TransactionServiceImpl {

    private final TransactionRepository transactionRepository;
    private final CursorUtils cursorUtils;

    public TransactionServiceImpl(TransactionRepository transactionRepository, CursorUtils cursorUtils) {
        this.transactionRepository = transactionRepository;
        this.cursorUtils = cursorUtils;
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

        List<TransactionViewResponseDto> dtos = window.stream()
                .map(this::mapToDto)
                .toList();

        String nextCursor = null;
        if (window.hasNext()) {
            ScrollPosition nextPos = window.positionAt(dtos.size() - 1);
            nextCursor = cursorUtils.encode(nextPos);
        }

        return new CursorResponse<>(dtos, window.hasNext(), nextCursor);
    }

    public TransactionDto getTransactionById(Jwt principial, UUID transactionId){
        UUID userId = UUID.fromString(principial.getSubject());
        Transaction transaction = transactionRepository.findByTransactionIdAndUserIdAndIsActiveTrue(transactionId, userId)
        .get();

        return mapToResponseDto(transaction);
    }
    private TransactionViewResponseDto mapToDto(Transaction t) {
        return new TransactionViewResponseDto(
                t.getTransactionId(),
                t.getAmount(),
                t.getTransactionName(),
                t.getIsoCurrencyCode(),
                t.getDetailedCategory().getDisplayName(),
                t.getDetailedCategory().getTransactionPrimaryCategory().getDisplayName());
    }

private TransactionDto mapToResponseDto(Transaction t) {
    if (t == null) {
        return null;
    }

    TransactionPrimaryCategoryDto primaryCategoryDto = new TransactionPrimaryCategoryDto(
        t.getDetailedCategory().getTransactionPrimaryCategory().getTransactionPrimaryCategoryId(),
        t.getDetailedCategory().getTransactionPrimaryCategory().getCategoryCode(),
        t.getDetailedCategory().getTransactionPrimaryCategory().getDisplayName(),
        t.getDetailedCategory().getTransactionPrimaryCategory().getIconUrl()
    );

    TransactionDetailedCategoryDto detailedCategoryDto = new TransactionDetailedCategoryDto(
        t.getDetailedCategory().getTransactionDetailedCategoryId(),
        t.getDetailedCategory().getDisplayName(),
        t.getDetailedCategory().getDetailedCode(),
        primaryCategoryDto
    );

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
        t.getUpdatedAt()
    );
}

}