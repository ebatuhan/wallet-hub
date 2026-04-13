package com.batu.account_service.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.batu.account_service.enums.AccountSortField;
import com.batu.account_service.service.AccountService;
import com.batu.shared.dto.request.AccountNameRequestDto;
import com.batu.shared.dto.response.AccountNameResponseDto;
import com.batu.shared.dto.response.AccountResponseDto;
import com.batu.shared.dto.response.AccountSummaryResponseDto;
import com.batu.shared.dto.response.AccountViewDto;
import com.batu.shared.dto.response.CursorResponse;

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

    @GetMapping("/summary")
    public ResponseEntity<AccountSummaryResponseDto> getAccountSummary(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountService.getAccountSummary(jwt));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('ROLE_SERVICE')")
    public ResponseEntity<List<AccountNameResponseDto>> getAccountsByGivenIds(@RequestBody AccountNameRequestDto request){
        return ResponseEntity.ok(accountService.getAccountsByGivenIds(request));
    } 


    @GetMapping
    public ResponseEntity<CursorResponse<AccountViewDto>> getAllAccounts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String institutionName,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String accountSubtype,
            @RequestParam(required = false) String cursor,

            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Limit must be at least 1") @Max(value = 100, message = "Limit cannot exceed 100") int limit,

            @RequestParam(required = false) AccountSortField sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {

        CursorResponse<AccountViewDto> response = accountService.getAccountsViewPaginated(
                jwt,
                accountName,
                institutionName,
                accountType,
                accountSubtype,
                cursor,
                limit,
                sortBy,
                direction);

        return ResponseEntity.ok(response);
    }
}
