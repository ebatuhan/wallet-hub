package com.batu.budgeting.mapper;

import org.springframework.stereotype.Component;

import com.batu.budgeting.dto.BudgetResponse;
import com.batu.budgeting.entity.Budget;
import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;

@Component
public class BudgetMapper {

    public BudgetResponse toResponse(Budget budget, TransactionPrimaryCategoryDto category) {
        return new BudgetResponse(
                budget.getId(),
                budget.getCategoryId(),
                category == null ? null : category.getCategoryCode(),
                category == null ? null : category.getDisplayName(),
                category == null ? null : category.getIconUrl(),
                budget.getLimitAmount(),
                budget.getSpentAmount(),
                budget.getIsoCurrencyCode(),
                budget.getPeriod(),
                budget.getPeriodStart(),
                budget.getPeriod().computeEnd(budget.getPeriodStart()),
                budget.isActive());
    }
}
