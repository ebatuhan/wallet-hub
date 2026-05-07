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

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class TransactionInsightsController {

    private final TransactionInsightsService transactionInsightsService;

    @GetMapping("/spendings")
    public ResponseEntity<SpendingPerCategoryResponseDto> getSpendingByCategory(
            @RequestParam Date from,
            @RequestParam Date to,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(transactionInsightsService.getSpendingByCategory(from, to, principal));
    }

    @GetMapping("/spendings/graph")
    public ResponseEntity<SpendingGraphResponseDto> getSpendingGraph(
            @RequestParam Date from,
            @RequestParam Date to,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(transactionInsightsService.getSpendingGraph(from, to, principal));
    }

    @GetMapping("/income")
    public ResponseEntity<IncomeSummaryResponseDto> getIncome(
            @RequestParam Date from,
            @RequestParam Date to,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(transactionInsightsService.getIncome(from, to, principal));
    }

    @GetMapping("/spendings/{accountId}")
    public ResponseEntity<SpendingPerCategoryByAccountResponseDto> getSpendingByCategoryByAccount(
            @RequestParam Date from,
            @PathVariable UUID accountId,
            @RequestParam Date to,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(transactionInsightsService.getSpendingPerCategoryByAccount(from, to, accountId, principal));
    }

    @GetMapping("/spendings/graph/{accountId}")
    public ResponseEntity<SpendingGraphResponseDto> getSpendingGraphByAccount(
            @RequestParam Date from,
            @PathVariable UUID accountId,
            @RequestParam Date to,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(transactionInsightsService.getSpendingGraphByAccount(from, to, accountId, principal));
    }
}
