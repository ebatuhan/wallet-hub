package com.batu.budgeting.unit.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.batu.budgeting.entity.BudgetPeriod;

class BudgetPeriodTest {

    @Test
    void computeEnd_whenPeriodIsWeekly_shouldReturnInclusiveSevenDayPeriodEnd() {
        LocalDate periodStart = LocalDate.of(2026, 5, 4);

        LocalDate periodEnd = BudgetPeriod.WEEKLY.computeEnd(periodStart);

        assertThat(periodEnd).isEqualTo(LocalDate.of(2026, 5, 10));
    }

    @Test
    void computeEnd_whenPeriodIsMonthly_shouldReturnInclusiveOneMonthPeriodEnd() {
        LocalDate periodStart = LocalDate.of(2026, 2, 1);

        LocalDate periodEnd = BudgetPeriod.MONTHLY.computeEnd(periodStart);

        assertThat(periodEnd).isEqualTo(LocalDate.of(2026, 2, 28));
    }
}
