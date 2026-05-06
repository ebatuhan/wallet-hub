package com.batu.budgeting.controller;

import java.util.UUID;

import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;

import com.batu.budgeting.dto.BudgetResponse;
import com.batu.budgeting.dto.CreateBudgetRequest;
import com.batu.budgeting.enums.BudgetSortField;
import com.batu.budgeting.service.BudgetService;
import com.batu.shared.dto.response.CursorResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

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
    public ResponseEntity<CursorResponse<BudgetResponse>> getBudgets(
            @AuthenticationPrincipal Jwt principal,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "Limit must be at least 1") @Max(value = 100, message = "Limit cannot exceed 100") int limit,
            @RequestParam(required = false) BudgetSortField sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ResponseEntity.ok(budgetService.getBudgets(principal, cursor, limit, sortBy, direction));
    }

    @DeleteMapping("/{budgetId}")
    public ResponseEntity<Void> deactivateBudget(
            @PathVariable UUID budgetId,
            @AuthenticationPrincipal Jwt principal) {
        budgetService.deactivateBudget(budgetId, principal);
        return ResponseEntity.noContent().build();
    }
}
