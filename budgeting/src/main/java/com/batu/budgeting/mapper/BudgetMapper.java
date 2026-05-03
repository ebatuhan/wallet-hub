package com.batu.budgeting.mapper;

import org.springframework.stereotype.Component;

import com.batu.budgeting.dto.BudgetResponse;
import com.batu.budgeting.entity.Budget;

@Component
public class BudgetMapper {

    public BudgetResponse toResponse(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getCategoryId(),
                budget.getLimitAmount(),
                budget.getSpentAmount(),
                budget.getIsoCurrencyCode(),
                budget.getPeriod(),
                budget.getPeriodStart(),
                budget.getPeriod().computeEnd(budget.getPeriodStart()),
                budget.isActive());
    }
}
