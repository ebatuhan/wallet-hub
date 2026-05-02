package com.batu.budgeting.service;

import java.util.List;
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.budgeting.dto.BudgetResponse;
import com.batu.budgeting.dto.CreateBudgetRequest;
import com.batu.budgeting.service.input.ApplyTransactionInput;

public interface BudgetService {
    BudgetResponse createBudget(CreateBudgetRequest request, Jwt principal);

    BudgetResponse createBudget(CreateBudgetRequest request, UUID userId);

    BudgetResponse updateBudget(UUID budgetId, CreateBudgetRequest request, Jwt principal);

    BudgetResponse updateBudget(UUID budgetId, CreateBudgetRequest request, UUID userId);

    List<BudgetResponse> getBudgets(Jwt principal);

    List<BudgetResponse> getBudgets(UUID userId);

    void deactivateBudget(UUID budgetId, Jwt principal);

    void deactivateBudget(UUID budgetId, UUID userId);

    void applyTransaction(ApplyTransactionInput input);
}
