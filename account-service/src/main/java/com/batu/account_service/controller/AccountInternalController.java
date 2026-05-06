package com.batu.account_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.batu.account_service.service.AccountService;
import com.batu.shared.dto.request.AccountUpsertRequestDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountUpsertResponseDto;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts/internal")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_service')")
public class AccountInternalController {
    private final AccountService accountService;

    @GetMapping("/by-connection/{connectionId}")
    public ResponseEntity<List<AccountResponseDto>> findAccountsByConnectionId(@PathVariable UUID connectionId) {
        return ResponseEntity.ok(accountService.findAccountsByConnectionId(connectionId));
    }

    @PostMapping("/upsert")
    public ResponseEntity<AccountUpsertResponseDto> upsertAccount(@Valid @RequestBody AccountUpsertRequestDto request) {
        return ResponseEntity.ok(accountService.upsertAccount(request));
    }

    @PutMapping("/deactivate-by-connection/{connectionId}")
    public ResponseEntity<List<AccountUpsertResponseDto>> deactivateAccountsByConnection(@PathVariable UUID connectionId) {
        return ResponseEntity.ok(accountService.deactivateAccountsByConnection(connectionId));
    }
}
