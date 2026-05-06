package com.batu.budgeting.unit.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.batu.budgeting.entity.BudgetPeriod;

class BudgetPeriodTest {

    @ParameterizedTest
    @CsvSource({
            "WEEKLY, 2026-05-04, 2026-05-10",
            "WEEKLY, 2026-12-29, 2027-01-04",
            "MONTHLY, 2026-02-01, 2026-02-28",
            "MONTHLY, 2024-02-01, 2024-02-29",
            "MONTHLY, 2026-12-15, 2027-01-14",
            "MONTHLY, 2026-01-31, 2026-02-27"
    })
    void computeEnd_shouldReturnInclusivePeriodEnd(BudgetPeriod period, LocalDate periodStart, LocalDate expectedEnd) {
        LocalDate periodEnd = period.computeEnd(periodStart);

        assertThat(periodEnd).isEqualTo(expectedEnd);
    }
}
