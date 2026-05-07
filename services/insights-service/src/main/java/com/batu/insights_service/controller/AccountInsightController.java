package com.batu.insights_service.controller;

import com.batu.insights_service.service.AccountInsightsService;
import com.batu.shared.dto.response.AccountBalanceDataPointDto;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class AccountInsightController {

    private final AccountInsightsService accountInsightService;

    @GetMapping("/accounts/{accountId}/balance-history")
    public ResponseEntity<List<AccountBalanceDataPointDto>> getAccountBalanceHistory(
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
