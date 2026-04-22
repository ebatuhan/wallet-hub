package com.batu.budgeting.exception;

import org.springframework.http.HttpStatus;

import com.batu.shared.exception.AbstractApplicationException;

public class BudgetConflictException extends AbstractApplicationException {
    public BudgetConflictException(String message) {
        super("BUDGET_CONFLICT", message, HttpStatus.CONFLICT);
    }
}
