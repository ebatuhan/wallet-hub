package com.batu.insights_service.controller;

import com.batu.insights_service.dto.AccountBalanceDataPointDTO;
import com.batu.insights_service.service.AccountInsightsService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/insights")
public class AccountInsightController {

    private final AccountInsightsService accountInsightService;

    public AccountInsightController(AccountInsightsService accountInsightService) {
        this.accountInsightService = accountInsightService;
    }

    @GetMapping("/accounts/{accountId}/balance-history")
    public ResponseEntity<List<AccountBalanceDataPointDTO>> getAccountBalanceHistory(
            @PathVariable UUID accountId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @AuthenticationPrincipal Jwt principal
    ) {
        return ResponseEntity.ok(
                accountInsightService.getAccountBalanceHistory(accountId, from, to, principal)
        );
    }
}