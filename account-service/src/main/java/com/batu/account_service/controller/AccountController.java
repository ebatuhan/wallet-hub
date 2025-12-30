package com.batu.account_service.controller;

import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated; 
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.batu.account_service.CursorResponse;
import com.batu.account_service.dto.AccountResponseDto;
import com.batu.account_service.enums.AccountSortField;
import com.batu.account_service.service.AccountService;

@RestController
@RequestMapping("/api/accounts")
@Validated 
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public CursorResponse<AccountResponseDto> getAllAccounts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String accountName,
            @RequestParam(required = false) String connectionId,
            @RequestParam(required = false) String accountType,
            @RequestParam(required = false) String accountSubtype,
            @RequestParam(required = false) String cursor,
            
            @RequestParam(required = false, defaultValue = "10") 
            @Min(value = 1, message = "Limit must be at least 1") 
            @Max(value = 100, message = "Limit cannot exceed 100") 
            int limit,

            @RequestParam(required = false) AccountSortField sortBy,
            @RequestParam(required = false, defaultValue = "DESC") Sort.Direction direction) {
        
        return accountService.getAccountsPaginated(
                jwt, accountName, connectionId, accountType, accountSubtype, cursor,
                limit, sortBy, direction);
    }
}