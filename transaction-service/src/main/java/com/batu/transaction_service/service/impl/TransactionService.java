package com.batu.transaction_service.service.impl;

import com.batu.transaction_service.dto.CursorResponse;
import com.batu.transaction_service.dto.TransactionViewResponseDto;
import com.batu.transaction_service.repository.projection.TransactionProjection;
import com.batu.transaction_service.repository.TransactionRepository;
import com.batu.transaction_service.repository.spec.TransactionSpecs;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public CursorResponse<TransactionViewResponseDto> getTransactions(
            String userId,
            String search,              // Added argument
            String categoryDisplayName, // Added argument
            String nextCursor, 
            int limit
    ) {
        // 1. Create Specification with all filters
        var spec = TransactionSpecs.withDynamicFilter(userId, search, categoryDisplayName);

        // 2. Determine Scroll Position (Manual UUID handling)
        ScrollPosition position;
        if (nextCursor == null || nextCursor.isBlank()) {
            position = ScrollPosition.keyset();
        } else {
            UUID id = UUID.fromString(nextCursor);
            position = ScrollPosition.forward(Map.of("transactionId", id));
        }

        // 3. Execute Query using Fluent API
        Window<TransactionProjection> window = transactionRepository.findBy(
                spec,
                q -> q.as(TransactionProjection.class)
                      .sortBy(Sort.by(Sort.Direction.DESC, "transactionId")) 
                      .limit(limit)
                      .scroll(position)
        );

        // 4. Map Projection to DTO
        List<TransactionViewResponseDto> dtos = window.stream()
                .map(p -> new TransactionViewResponseDto(
                        p.getTransactionId(),
                        p.getAmount(),
                        p.getTransactionName(),
                        p.getIsoCurrencyCode(),
                        p.getCategoryDisplayName(),
                        p.getDetailedCategoryName()
                ))
                .collect(Collectors.toList());

        // 5. Build Response
        String newCursor = null;
        if (window.hasNext() && !dtos.isEmpty()) {
            newCursor = dtos.get(dtos.size() - 1).getTransactionId().toString();
        }

        return new CursorResponse<>(
                dtos,
                window.hasNext(),
                newCursor
        );
    }
}