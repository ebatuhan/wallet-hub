package com.batu.transaction_service.unit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.entity.TransactionDetailedCategory;
import com.batu.transaction_service.entity.TransactionPrimaryCategory;
import com.batu.transaction_service.mapper.TransactionSyncMapper;

class TransactionSyncMapperTest {

    private static final UUID TRANSACTION_ID = UUID.fromString("e6000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("e6000000-0000-0000-0000-000000000002");
    private static final UUID ACCOUNT_ID = UUID.fromString("e6000000-0000-0000-0000-000000000003");
    private static final UUID PRIMARY_CATEGORY_ID = UUID.fromString("e6000000-0000-0000-0000-000000000004");
    private static final UUID DETAILED_CATEGORY_ID = UUID.fromString("e6000000-0000-0000-0000-000000000005");

    @Test
    void toDto_whenTransactionHasCategoryTree_shouldMapTransactionAndCategoryFields() {
        Transaction transaction = transaction();

        var response = new TransactionSyncMapper().toDto(transaction);

        assertThat(response.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(response.getAmount()).isEqualByComparingTo("42.50");
        assertThat(response.getIsoCurrencyCode()).isEqualTo("USD");
        assertThat(response.getTransactionName()).isEqualTo("Coffee Shop");
        assertThat(response.getDetailedCategory().getTransactionDetailedCategoryId()).isEqualTo(DETAILED_CATEGORY_ID);
        assertThat(response.getDetailedCategory().getDetailedCode()).isEqualTo("FOOD_AND_DRINK_COFFEE");
        assertThat(response.getDetailedCategory().getPrimaryCategory().getTransactionPrimaryCategoryId()).isEqualTo(PRIMARY_CATEGORY_ID);
        assertThat(response.getDetailedCategory().getPrimaryCategory().getCategoryCode()).isEqualTo("FOOD_AND_DRINK");
    }

    private Transaction transaction() {
        TransactionPrimaryCategory primaryCategory = new TransactionPrimaryCategory("FOOD_AND_DRINK", "Food & Drink", "food.svg");
        ReflectionTestUtils.setField(primaryCategory, "transactionPrimaryCategoryId", PRIMARY_CATEGORY_ID);
        TransactionDetailedCategory detailedCategory = new TransactionDetailedCategory(
                "Coffee",
                "FOOD_AND_DRINK_COFFEE",
                primaryCategory,
                "Coffee shops");
        ReflectionTestUtils.setField(detailedCategory, "transactionDetailedCategoryId", DETAILED_CATEGORY_ID);
        return new Transaction(
                TRANSACTION_ID,
                USER_ID,
                ACCOUNT_ID,
                new BigDecimal("42.50"),
                "USD",
                "Coffee Shop",
                "place",
                LocalDate.of(2026, 5, 7),
                false,
                "in store",
                detailedCategory,
                true);
    }
}
