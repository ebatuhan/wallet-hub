package com.batu.transaction_service;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batu.transaction_service.dto.CursorResponse;
import com.batu.transaction_service.dto.TransactionDto;
import com.batu.transaction_service.dto.TransactionViewResponseDto;
import com.batu.transaction_service.service.impl.TransactionServiceImpl;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionServiceImpl transactionService;

    public TransactionController(TransactionServiceImpl transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<CursorResponse<TransactionViewResponseDto>> getTransactions(
            @AuthenticationPrincipal Jwt principal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int limit) {

        CursorResponse<TransactionViewResponseDto> response = transactionService.transatcions(
                principal,
                category,
                cursor,
                limit);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionDto> getTransactionById(
            @AuthenticationPrincipal Jwt principal,
            @PathVariable UUID transactionId) {
        return ResponseEntity.ok(transactionService.getTransactionById(principal, transactionId));
    }

}
