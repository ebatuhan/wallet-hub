package com.batu.budgeting.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import com.batu.budgeting.dto.BudgetResponse;
import com.batu.budgeting.dto.CreateBudgetRequest;
import com.batu.budgeting.service.BudgetService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> createBudget(
            @Valid @RequestBody CreateBudgetRequest request,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(budgetService.createBudget(request, principal));
    }

    @PutMapping("/{budgetId}")
    public ResponseEntity<BudgetResponse> updateBudget(
            @PathVariable UUID budgetId,
            @Valid @RequestBody CreateBudgetRequest request,
            @AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(budgetService.updateBudget(budgetId, request, principal));
    }

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getBudgets(@AuthenticationPrincipal Jwt principal) {
        return ResponseEntity.ok(budgetService.getBudgets(principal));
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deactivateBudget(
            @PathVariable UUID budgetId,
            @AuthenticationPrincipal Jwt principal) {
        budgetService.deactivateBudget(budgetId, principal);
        return ResponseEntity.noContent().build();
    }
}
