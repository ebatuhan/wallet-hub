package com.batu.dashboard_service.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batu.dashboard_service.dto.AccountDashboardSummaryResponseDto;
import com.batu.dashboard_service.dto.TransactionDashboardSummaryResponseDto;
import com.batu.dashboard_service.dto.UserDashboardSummaryResponseDto;
import com.batu.dashboard_service.service.DashboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<UserDashboardSummaryResponseDto> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer recentLimit,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(dashboardService.getUserSummary(from, to, recentLimit, principal));
    }

    @GetMapping("/accounts/{accountId}/summary")
    public ResponseEntity<AccountDashboardSummaryResponseDto> getAccountSummary(
            @PathVariable UUID accountId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(dashboardService.getAccountSummary(accountId, from, to, limit, cursor, principal));
    }

    @GetMapping("/transactions/{transactionId}/summary")
    public ResponseEntity<TransactionDashboardSummaryResponseDto> getTransactionSummary(
            @PathVariable UUID transactionId,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(dashboardService.getTransactionSummary(transactionId, principal));
    }
}
