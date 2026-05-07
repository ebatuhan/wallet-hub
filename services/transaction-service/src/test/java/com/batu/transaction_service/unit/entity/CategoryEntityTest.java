package com.batu.transaction_service.unit.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.batu.transaction_service.entity.TransactionDetailedCategory;
import com.batu.transaction_service.entity.TransactionPrimaryCategory;

class CategoryEntityTest {

    @Test
    void primaryCategoryConstructor_shouldPopulateFields() {
        TransactionPrimaryCategory category = new TransactionPrimaryCategory("FOOD_AND_DRINK", "Food & Drink", "food.svg");

        assertThat(category.getCategoryCode()).isEqualTo("FOOD_AND_DRINK");
        assertThat(category.getDisplayName()).isEqualTo("Food & Drink");
        assertThat(category.getIconUrl()).isEqualTo("food.svg");
    }

    @Test
    void detailedCategoryConstructor_shouldPopulateFields() {
        TransactionPrimaryCategory primaryCategory = new TransactionPrimaryCategory("FOOD_AND_DRINK", "Food & Drink", "food.svg");

        TransactionDetailedCategory category = new TransactionDetailedCategory(
                "Coffee",
                "FOOD_AND_DRINK_COFFEE",
                primaryCategory,
                "Coffee shops");

        assertThat(category.getDisplayName()).isEqualTo("Coffee");
        assertThat(category.getCategoryCode()).isEqualTo("FOOD_AND_DRINK_COFFEE");
        assertThat(category.getTransactionPrimaryCategory()).isSameAs(primaryCategory);
        assertThat(category.getDescription()).isEqualTo("Coffee shops");
    }
}
