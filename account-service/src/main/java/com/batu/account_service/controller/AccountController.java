package com.batu.account_service.controller;

import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.batu.account_service.CursorResponse;
import com.batu.account_service.dto.AccountResponseDto;
import com.batu.account_service.dto.AccountViewDto;
import com.batu.account_service.enums.AccountSortField;
import com.batu.account_service.service.AccountService;

@RestController
@RequestMapping("/accounts")
@Validated
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponseDto> getAccount(
            @PathVariable UUID accountId,
            @AuthenticationPrincipal Jwt jwt) {
        AccountResponseDto account = accountService.getAccount(accountId, jwt);
        return ResponseEntity.ok(account);
    }

    @GetMapping
    public ResponseEntity<CursorResponse<AccountViewDto>> getAllAccounts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String connectionId,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String accountSubtype,
            @RequestParam(required = false) String cursor,

            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Limit must be at least 1") @Max(value = 100, message = "Limit cannot exceed 100") int limit,

            @RequestParam(required = false) AccountSortField sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        CursorResponse<AccountViewDto> response = accountService.getAccountsViewPaginated(
                jwt,
                accountName,
                connectionId,
                accountType,
                accountSubtype,
                cursor,
                limit,
                sortBy,
                direction);

        return ResponseEntity.ok(response);
    }
}
