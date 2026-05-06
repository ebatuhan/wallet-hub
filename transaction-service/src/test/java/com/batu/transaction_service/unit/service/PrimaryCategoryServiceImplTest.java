package com.batu.transaction_service.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.batu.transaction_service.entity.TransactionPrimaryCategory;
import com.batu.transaction_service.repository.PrimaryCategoryRepository;
import com.batu.transaction_service.service.impl.PrimaryCategoryServiceImpl;

@ExtendWith(MockitoExtension.class)
class PrimaryCategoryServiceImplTest {

    private static final UUID CATEGORY_ID = UUID.fromString("e2000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_CATEGORY_ID = UUID.fromString("e2000000-0000-0000-0000-000000000002");

    @Mock
    private PrimaryCategoryRepository primaryCategoryRepository;

    @Test
    void getByCategoryCode_whenCategoryExists_shouldReturnCategory() {
        TransactionPrimaryCategory category = category(CATEGORY_ID, "FOOD_AND_DRINK", "Food & Drink");
        when(primaryCategoryRepository.findByCategoryCode("FOOD_AND_DRINK")).thenReturn(Optional.of(category));

        var response = new PrimaryCategoryServiceImpl(primaryCategoryRepository).getByCategoryCode("FOOD_AND_DRINK");

        assertThat(response).isSameAs(category);
    }

    @Test
    void getByCategoryCode_whenCategoryIsMissing_shouldThrowNotFound() {
        when(primaryCategoryRepository.findByCategoryCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new PrimaryCategoryServiceImpl(primaryCategoryRepository).getByCategoryCode("UNKNOWN"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Primary category with code UNKNOWN not found");
                });
    }

    @Test
    void getById_whenCategoryExists_shouldReturnCategory() {
        TransactionPrimaryCategory category = category(CATEGORY_ID, "TRAVEL", "Travel");
        when(primaryCategoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

        var response = new PrimaryCategoryServiceImpl(primaryCategoryRepository).getById(CATEGORY_ID);

        assertThat(response).isSameAs(category);
    }

    @Test
    void getById_whenCategoryIsMissing_shouldThrowNotFound() {
        when(primaryCategoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new PrimaryCategoryServiceImpl(primaryCategoryRepository).getById(CATEGORY_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Primary category with id " + CATEGORY_ID + " not found");
                });
    }

    @Test
    void getAll_whenCategoriesExist_shouldReturnMappedDtos() {
        when(primaryCategoryRepository.findAll()).thenReturn(List.of(
                category(CATEGORY_ID, "FOOD_AND_DRINK", "Food & Drink"),
                category(OTHER_CATEGORY_ID, "TRAVEL", "Travel")));

        var response = new PrimaryCategoryServiceImpl(primaryCategoryRepository).getAll();

        assertThat(response).hasSize(2);
        assertThat(response).extracting("categoryCode").containsExactly("FOOD_AND_DRINK", "TRAVEL");
        assertThat(response.getFirst().getTransactionPrimaryCategoryId()).isEqualTo(CATEGORY_ID);
    }

    @Test
    void getByIds_whenIdsAreProvided_shouldReturnMappedDtos() {
        Set<UUID> ids = Set.of(CATEGORY_ID, OTHER_CATEGORY_ID);
        when(primaryCategoryRepository.findByTransactionPrimaryCategoryIdIn(ids)).thenReturn(List.of(
                category(CATEGORY_ID, "FOOD_AND_DRINK", "Food & Drink"),
                category(OTHER_CATEGORY_ID, "TRAVEL", "Travel")));

        var response = new PrimaryCategoryServiceImpl(primaryCategoryRepository).getByIds(ids);

        assertThat(response).extracting("transactionPrimaryCategoryId")
                .containsExactly(CATEGORY_ID, OTHER_CATEGORY_ID);
    }

    private TransactionPrimaryCategory category(UUID id, String code, String name) {
        TransactionPrimaryCategory category = new TransactionPrimaryCategory(code, name, code.toLowerCase() + ".svg");
        ReflectionTestUtils.setField(category, "transactionPrimaryCategoryId", id);
        return category;
    }
}
