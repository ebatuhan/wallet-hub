package com.batu.insights_service.controller;

import java.sql.Date;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batu.insights_service.service.TransactionInsightsService;
import com.batu.shared.dto.response.IncomeSummaryResponseDto;
import com.batu.shared.dto.response.SpendingGraphResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryByAccountResponseDto;
import com.batu.shared.dto.response.SpendingPerCategoryResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
@Tag(name = "Transaction Insights", description = "Spending, income, and graph insight endpoints.")
@SecurityRequirement(name = "bearerAuth")
public class TransactionInsightsController {

    private final TransactionInsightsService transactionInsightsService;

    @GetMapping("/spendings")
    @Operation(summary = "Get spending by category", description = "Returns spending grouped by category for a year (from=YYYY) or month (from=YYYY-MM).")
    public ResponseEntity<SpendingPerCategoryResponseDto> getSpendingByCategory(
            @Parameter(description = "Spending period. Use YYYY for yearly totals or YYYY-MM for monthly totals.", example = "2025-04")
            @RequestParam String from,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(transactionInsightsService.getSpendingByCategory(from, principal));
    }

    @GetMapping("/spendings/graph")
    @Operation(summary = "Get spending graph", description = "Returns monthly datapoints for from=YYYY and weekly datapoints for from=YYYY-MM.")
    public ResponseEntity<SpendingGraphResponseDto> getSpendingGraph(
            @Parameter(description = "Spending period. Use YYYY for monthly graph buckets or YYYY-MM for weekly graph buckets.", example = "2025")
            @RequestParam String from,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(transactionInsightsService.getSpendingGraph(from, principal));
    }

    @GetMapping("/income")
    @Operation(summary = "Get income summary", description = "Returns income summary data for a date range.")
    public ResponseEntity<IncomeSummaryResponseDto> getIncome(
            @RequestParam Date from,
            @RequestParam Date to,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(transactionInsightsService.getIncome(from, to, principal));
    }

    @GetMapping("/spendings/{accountId}")
    @Operation(summary = "Get account spending by category", description = "Returns account spending grouped by category for a year (from=YYYY) or month (from=YYYY-MM).")
    public ResponseEntity<SpendingPerCategoryByAccountResponseDto> getSpendingByCategoryByAccount(
            @Parameter(description = "Spending period. Use YYYY for yearly totals or YYYY-MM for monthly totals.", example = "2025-04")
            @RequestParam String from,
            @PathVariable UUID accountId,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(transactionInsightsService.getSpendingPerCategoryByAccount(from, accountId, principal));
    }

    @GetMapping("/spendings/graph/{accountId}")
    @Operation(summary = "Get account spending graph", description = "Returns account monthly datapoints for from=YYYY and weekly datapoints for from=YYYY-MM.")
    public ResponseEntity<SpendingGraphResponseDto> getSpendingGraphByAccount(
            @Parameter(description = "Spending period. Use YYYY for monthly graph buckets or YYYY-MM for weekly graph buckets.", example = "2025")
            @RequestParam String from,
            @PathVariable UUID accountId,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(transactionInsightsService.getSpendingGraphByAccount(from, accountId, principal));
    }
}
