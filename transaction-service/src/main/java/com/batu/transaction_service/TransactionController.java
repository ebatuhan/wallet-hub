package com.batu.transaction_service;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batu.shared.dto.request.AccountIdsRequestDto;
import com.batu.shared.dto.request.TransactionsUpsertRequestDto;
import com.batu.transaction_service.service.impl.TransactionServiceImpl;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.TransactionDto;
import com.batu.shared.dto.response.TransactionViewResponseDto;

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
            @RequestParam(required = false) UUID accountId,
            @RequestParam(defaultValue = "10") int limit) {

        CursorResponse<TransactionViewResponseDto> response = transactionService.transatcions(
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

    @PostMapping("/sync/batch-save")
    @PreAuthorize("hasAuthority('ROLE_SERVICE')")
    public ResponseEntity<Void> saveSyncedTransactions(@RequestBody TransactionsUpsertRequestDto request) {
        transactionService.saveBatch(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sync/deactivate-accounts")
    @PreAuthorize("hasAuthority('ROLE_SERVICE')")
    public ResponseEntity<Void> deactivateTransactionsByAccountIds(@RequestBody AccountIdsRequestDto request) {
        transactionService.deactivateByAccountIds(request);
        return ResponseEntity.noContent().build();
    }
}
