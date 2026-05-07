package com.batu.budgeting.unit.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.batu.budgeting.enums.BudgetSortField;

class BudgetSortFieldTest {

    @ParameterizedTest
    @CsvSource({
            "CREATED_AT, createdAt",
            "UPDATED_AT, updatedAt",
            "PERIOD_START, periodStart",
            "LIMIT_AMOUNT, limitAmount",
            "SPENT_AMOUNT, spentAmount"
    })
    void getFieldName_shouldExposeEntityFieldNamesUsedByRepositorySorting(BudgetSortField sortField, String fieldName) {
        assertThat(sortField.getFieldName()).isEqualTo(fieldName);
    }
}
