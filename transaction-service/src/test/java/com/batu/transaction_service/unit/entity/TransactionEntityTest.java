package com.batu.transaction_service.unit.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.entity.TransactionDetailedCategory;
import com.batu.transaction_service.entity.TransactionPrimaryCategory;

class TransactionEntityTest {

    private static final UUID TRANSACTION_ID = UUID.fromString("e7000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("e7000000-0000-0000-0000-000000000002");
    private static final UUID ACCOUNT_ID = UUID.fromString("e7000000-0000-0000-0000-000000000003");

    @Test
    void constructorWithId_shouldPopulateTransactionFields() {
        TransactionDetailedCategory category = detailedCategory();

        Transaction transaction = new Transaction(
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
                category,
                true);

        assertThat(transaction.getId()).isEqualTo(TRANSACTION_ID);
        assertThat(transaction.getUserId()).isEqualTo(USER_ID);
        assertThat(transaction.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(transaction.getAmount()).isEqualByComparingTo("42.50");
        assertThat(transaction.getIsoCurrencyCode()).isEqualTo("USD");
        assertThat(transaction.getTransactionName()).isEqualTo("Coffee Shop");
        assertThat(transaction.getTransactionType()).isEqualTo("place");
        assertThat(transaction.getDate()).isEqualTo(LocalDate.of(2026, 5, 7));
        assertThat(transaction.getPending()).isFalse();
        assertThat(transaction.getPaymentChannel()).isEqualTo("in store");
        assertThat(transaction.getDetailedCategory()).isSameAs(category);
        assertThat(transaction.isActive()).isTrue();
    }

    @Test
    void isNew_whenCreatedAtIsMissing_shouldReturnTrue() {
        Transaction transaction = new Transaction(
                USER_ID,
                ACCOUNT_ID,
                BigDecimal.ONE,
                "USD",
                "Coffee Shop",
                "place",
                LocalDate.of(2026, 5, 7),
                false,
                "in store",
                detailedCategory(),
                true);

        assertThat(transaction.isNew()).isTrue();
    }

    @Test
    void isNew_whenCreatedAtExists_shouldReturnFalse() {
        Transaction transaction = new Transaction(
                TRANSACTION_ID,
                USER_ID,
                ACCOUNT_ID,
                BigDecimal.ONE,
                "USD",
                "Coffee Shop",
                "place",
                LocalDate.of(2026, 5, 7),
                false,
                "in store",
                detailedCategory(),
                true);
        ReflectionTestUtils.setField(transaction, "createdAt", Instant.parse("2026-05-07T00:00:00Z"));

        assertThat(transaction.isNew()).isFalse();
    }

    private TransactionDetailedCategory detailedCategory() {
        return new TransactionDetailedCategory(
                "Coffee",
                "FOOD_AND_DRINK_COFFEE",
                new TransactionPrimaryCategory("FOOD_AND_DRINK", "Food & Drink", "food.svg"),
                "Coffee shops");
    }
}
