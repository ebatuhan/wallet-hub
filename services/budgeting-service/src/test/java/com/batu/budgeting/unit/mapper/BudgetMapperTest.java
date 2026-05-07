package com.batu.budgeting.unit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.batu.budgeting.dto.BudgetResponse;
import com.batu.budgeting.entity.Budget;
import com.batu.budgeting.entity.BudgetPeriod;
import com.batu.budgeting.mapper.BudgetMapper;

class BudgetMapperTest {

    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CATEGORY_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

    private final BudgetMapper budgetMapper = new BudgetMapper();

    @Test
    void toResponse_whenBudgetIsMonthly_shouldMapFieldsAndComputedPeriodEnd() {
        Budget budget = new Budget(
                USER_ID,
                CATEGORY_ID,
                new BigDecimal("500.0000"),
                "USD",
                BudgetPeriod.MONTHLY,
                LocalDate.of(2026, 5, 1));
        budget.setSpentAmount(new BigDecimal("125.2500"));

        BudgetResponse response = budgetMapper.toResponse(budget);

        assertThat(response.id()).isNull();
        assertThat(response.categoryId()).isEqualTo(CATEGORY_ID);
        assertThat(response.limitAmount()).isEqualByComparingTo("500.0000");
        assertThat(response.spentAmount()).isEqualByComparingTo("125.2500");
        assertThat(response.isoCurrencyCode()).isEqualTo("USD");
        assertThat(response.period()).isEqualTo(BudgetPeriod.MONTHLY);
        assertThat(response.periodStart()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(response.periodEnd()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(response.active()).isTrue();
    }
}
