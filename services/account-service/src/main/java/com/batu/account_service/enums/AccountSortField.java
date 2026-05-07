package com.batu.account_service.enums;

public enum AccountSortField {
    ACCOUNT_NAME("accountName"),
    CREATED_AT("createdAt"),
    AVAILABLE_BALANCE("availableBalance"),
    CURRENT_BALANCE("currentBalance"),
    ACCOUNT_TYPE("accountType");

    private final String fieldName;

    AccountSortField(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
