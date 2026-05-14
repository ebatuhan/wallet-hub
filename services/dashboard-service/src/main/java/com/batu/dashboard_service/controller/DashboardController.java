package com.batu.dashboard_service.controller;

import java.util.UUID;

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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "User dashboard summary and drill-down endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(summary = "Get user dashboard summary", description = "Returns dashboard summary for a year (from=YYYY) or month (from=YYYY-MM). Omit from for the current month.")
    public ResponseEntity<UserDashboardSummaryResponseDto> getSummary(
            @Parameter(description = "Dashboard period. Use YYYY for yearly summary or YYYY-MM for monthly summary.", example = "2025-04")
            @RequestParam(required = false) String from,
            @RequestParam(required = false) @Min(value = 1, message = "Limit must be at least 1") @Max(value = 100, message = "Limit cannot exceed 100") Integer recentLimit,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(dashboardService.getUserSummary(from, recentLimit, principal));
    }

    @GetMapping("/budgets")
    @Operation(summary = "Get dashboard budgets", description = "Returns enriched budget data for dashboard views.")
    public ResponseEntity<CursorResponse<BudgetResponseDto>> getBudgets(
            @RequestParam(required = false) @Min(value = 1, message = "Limit must be at least 1") @Max(value = 100, message = "Limit cannot exceed 100") Integer limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(dashboardService.getBudgets(limit, cursor, sortBy, direction, principal));
    }

    @GetMapping("/accounts/{accountId}/summary")
    @Operation(summary = "Get account dashboard summary", description = "Returns account dashboard summary for a year (from=YYYY) or month (from=YYYY-MM). Omit from for the current month.")
    public ResponseEntity<AccountDashboardSummaryResponseDto> getAccountSummary(
            @PathVariable UUID accountId,
            @Parameter(description = "Dashboard period. Use YYYY for yearly summary or YYYY-MM for monthly summary.", example = "2025-04")
            @RequestParam(required = false) String from,
            @RequestParam(required = false) @Min(value = 1, message = "Limit must be at least 1") @Max(value = 100, message = "Limit cannot exceed 100") Integer limit,
            @RequestParam(required = false) String cursor,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(dashboardService.getAccountSummary(accountId, from, limit, cursor, principal));
    }

    @GetMapping("/transactions/{transactionId}/summary")
    @Operation(summary = "Get transaction dashboard summary", description = "Returns dashboard summary data for one transaction.")
    public ResponseEntity<TransactionDashboardSummaryResponseDto> getTransactionSummary(
            @PathVariable UUID transactionId,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(dashboardService.getTransactionSummary(transactionId, principal));
    }
}
