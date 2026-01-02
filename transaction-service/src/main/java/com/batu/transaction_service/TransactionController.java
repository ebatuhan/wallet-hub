package com.batu.transaction_service;

import com.batu.transaction_service.dto.CursorResponse;
import com.batu.transaction_service.dto.TransactionViewResponseDto;
import com.batu.transaction_service.service.impl.TransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public CursorResponse<TransactionViewResponseDto> getTransactions(
            @RequestParam String userId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category, // Maps to categoryDisplayName
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return transactionService.getTransactions(userId, search, category, cursor, limit);
    }
}