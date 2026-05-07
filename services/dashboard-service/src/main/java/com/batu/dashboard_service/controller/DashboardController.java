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
import com.batu.shared.dto.response.BudgetResponseDto;
import com.batu.shared.dto.response.CursorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "User dashboard summary and drill-down endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get user dashboard summary", description = "Returns user-level account, transaction, spending, and recent activity summary data.")
    public ResponseEntity<UserDashboardSummaryResponseDto> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer recentLimit,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(dashboardService.getUserSummary(from, to, recentLimit, principal));
    }

    @GetMapping("/budgets")
    @Operation(summary = "Get dashboard budgets", description = "Returns enriched budget data for dashboard views.")
    public ResponseEntity<CursorResponse<BudgetResponseDto>> getBudgets(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(dashboardService.getBudgets(limit, cursor, sortBy, direction, principal));
    }

    @GetMapping("/accounts/{accountId}/summary")
    @Operation(summary = "Get account dashboard summary", description = "Returns dashboard summary data for one account.")
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
    @Operation(summary = "Get transaction dashboard summary", description = "Returns dashboard summary data for one transaction.")
    public ResponseEntity<TransactionDashboardSummaryResponseDto> getTransactionSummary(
            @PathVariable UUID transactionId,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(dashboardService.getTransactionSummary(transactionId, principal));
    }
}
