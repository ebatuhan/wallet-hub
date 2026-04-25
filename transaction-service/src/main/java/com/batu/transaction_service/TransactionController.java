package com.batu.transaction_service;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batu.shared.dto.request.TransactionRequestDto;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.TransactionDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;
import com.batu.transaction_service.service.TransactionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<CursorResponse<TransactionViewResponseDto>> getTransactions(
            @AuthenticationPrincipal Jwt principal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) UUID accountId,
            @RequestParam(defaultValue = "10") int limit) {

        CursorResponse<TransactionViewResponseDto> response = transactionService.transactions(
                principal,
                category,
                accountId,
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

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody TransactionRequestDto request) {
        transactionService.create(request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{transactionId}")
    public ResponseEntity<Void> update(@PathVariable UUID transactionId, @RequestBody TransactionRequestDto request) {
        if (!transactionId.equals(request.getTransactionId())) {
            throw new IllegalArgumentException("Transaction id mismatch");
        }

        transactionService.update(request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/accounts/{accountId}")
    public ResponseEntity<Void> deactivateTransactionsByAccountId(@PathVariable UUID accountId) {
        transactionService.deactivateByAccountId(accountId);
        return ResponseEntity.noContent().build();
    }
}
