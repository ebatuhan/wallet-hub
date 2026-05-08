package com.batu.transaction_service.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.TransactionDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;
import com.batu.transaction_service.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Transaction browsing endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping
    @Operation(summary = "List transactions", description = "Returns cursor-paginated transactions with optional category and account filters.")
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
    @Operation(summary = "Get transaction", description = "Returns a single transaction owned by the authenticated user.")
    public ResponseEntity<TransactionDto> getTransactionById(
            @AuthenticationPrincipal Jwt principal,
            @PathVariable UUID transactionId) {
        return ResponseEntity.ok(transactionService.getTransactionById(principal, transactionId));
    }

}
