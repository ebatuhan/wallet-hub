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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
@Tag(name = "Account Insights", description = "Account balance insight endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class AccountInsightController {

    private final AccountInsightsService accountInsightService;

    @GetMapping("/accounts/{accountId}/balance-history")
    @Operation(summary = "Get account balance history", description = "Returns account balance history points for a date range.")
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
