package com.batu.transaction_service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batu.shared.dto.request.TransactionUpsertRequestDto;
import com.batu.shared.dto.response.TransactionUpsertResponseDto;
import com.batu.transaction_service.service.TransactionService;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/transactions/internal")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_service')")
@Hidden
public class TransactionInternalController {
    private final TransactionService transactionService;

    @PostMapping("/upsert")
    public ResponseEntity<TransactionUpsertResponseDto> upsertTransaction(@Valid @RequestBody TransactionUpsertRequestDto request) {
        return ResponseEntity.ok(transactionService.upsertTransaction(request));
    }

    @PutMapping("/deactivate-by-account/{accountId}")
    public ResponseEntity<List<TransactionUpsertResponseDto>> deactivateTransactionsByAccount(@PathVariable UUID accountId) {
        return ResponseEntity.ok(transactionService.deactivateTransactionsByAccountId(accountId));
    }
}
