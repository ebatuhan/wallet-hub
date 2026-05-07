package com.batu.budgeting.enums;

public enum BudgetSortField {
    CREATED_AT("createdAt"),
    UPDATED_AT("updatedAt"),
    PERIOD_START("periodStart"),
    LIMIT_AMOUNT("limitAmount"),
    SPENT_AMOUNT("spentAmount");

    private final String fieldName;

    BudgetSortField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
