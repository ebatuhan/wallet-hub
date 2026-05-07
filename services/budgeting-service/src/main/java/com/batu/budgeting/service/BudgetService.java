package com.batu.budgeting.service;

import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.Jwt;

import com.batu.budgeting.dto.BudgetResponse;
import com.batu.budgeting.dto.CreateBudgetRequest;
import com.batu.budgeting.enums.BudgetSortField;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.messaging.event.TransactionRecorded;

public interface BudgetService {
    BudgetResponse createBudget(CreateBudgetRequest request, Jwt principal);

    BudgetResponse createBudget(CreateBudgetRequest request, UUID userId);

    BudgetResponse updateBudget(UUID budgetId, CreateBudgetRequest request, Jwt principal);

    BudgetResponse updateBudget(UUID budgetId, CreateBudgetRequest request, UUID userId);

    CursorResponse<BudgetResponse> getBudgets(Jwt principal, String cursor, int limit, BudgetSortField sortBy,
            Sort.Direction direction);

    CursorResponse<BudgetResponse> getBudgets(UUID userId, String cursor, int limit, BudgetSortField sortBy,
            Sort.Direction direction);

    void deactivateBudget(UUID budgetId, Jwt principal);

    void deactivateBudget(UUID budgetId, UUID userId);

    void applyTransaction(TransactionRecorded transaction);
}
