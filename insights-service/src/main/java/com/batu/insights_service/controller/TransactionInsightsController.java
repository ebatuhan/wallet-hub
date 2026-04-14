package com.batu.insights_service.controller;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.batu.insights_service.dto.SpendingPerCategoryByAccountDTO;
import com.batu.insights_service.dto.SpendingPerCategoryByAccountResponseDTO;
import com.batu.insights_service.dto.SpendingPerCategoryDTO;
import com.batu.insights_service.dto.SpendingPerCategoryResponseDTO;
import com.batu.insights_service.dto.IncomeSummaryResponseDTO;
import com.batu.insights_service.service.TransactionInsightsService;

@RestController
@RequestMapping("/api/insights")
public class TransactionInsightsController {

    private final TransactionInsightsService transactionInsightsService;

    public TransactionInsightsController(TransactionInsightsService transactionInsightsService) {
        this.transactionInsightsService = transactionInsightsService;
    }

    @GetMapping("/spendings")
    public ResponseEntity<SpendingPerCategoryResponseDTO> getSpendingByCategory(
            @RequestParam Date from,
            @RequestParam Date to,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(transactionInsightsService.getSpendingByCategory(from, to, principal));
    }

    @GetMapping("/income")
    public ResponseEntity<IncomeSummaryResponseDTO> getIncome(
            @RequestParam Date from,
            @RequestParam Date to,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(transactionInsightsService.getIncome(from, to, principal));
    }

        @GetMapping("/spendings/{accountId}")
    public ResponseEntity<SpendingPerCategoryByAccountResponseDTO> getSpendingByCategoryByAccount(
            @RequestParam Date from,
            @PathVariable UUID accountId,
            @RequestParam Date to,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(transactionInsightsService.getSpendingPerCategoryByAccount(from, to, accountId, principal));
    }

}
