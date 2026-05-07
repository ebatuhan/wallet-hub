package com.batu.account_service.unit.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.batu.account_service.enums.AccountSortField;

class AccountSortFieldTest {

    @ParameterizedTest
    @CsvSource({
            "ACCOUNT_NAME, accountName",
            "CREATED_AT, createdAt",
            "AVAILABLE_BALANCE, availableBalance",
            "CURRENT_BALANCE, currentBalance",
            "ACCOUNT_TYPE, accountType"
    })
    void getFieldName_shouldExposeEntityFieldNamesUsedByRepositorySorting(AccountSortField sortField, String fieldName) {
        assertThat(sortField.getFieldName()).isEqualTo(fieldName);
    }
}
