package com.batu.budgeting.entity;

import java.time.LocalDate;

public enum BudgetPeriod {
    WEEKLY,
    MONTHLY;

    public LocalDate computeEnd(LocalDate start) {
        return switch (this) {
            case WEEKLY -> start.plusWeeks(1).minusDays(1);
            case MONTHLY -> start.plusMonths(1).minusDays(1);
        };
    }
}
