package com.batu.transaction_service.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.batu.transaction_service.entity.TransactionDetailedCategory;
import com.batu.transaction_service.entity.TransactionPrimaryCategory;
import com.batu.transaction_service.repository.DetailedCategoryRepository;
import com.batu.transaction_service.service.impl.DetailedCategoryServiceImpl;

@ExtendWith(MockitoExtension.class)
class DetailedCategoryServiceImplTest {

    private static final UUID DETAILED_CATEGORY_ID = UUID.fromString("e3000000-0000-0000-0000-000000000001");
    private static final UUID FALLBACK_CATEGORY_ID = UUID.fromString("e3000000-0000-0000-0000-000000000002");

    @Mock
    private DetailedCategoryRepository detailedCategoryRepository;

    @Test
    void getByCategoryCode_whenCategoryExists_shouldReturnCategoryAndCacheIt() {
        TransactionDetailedCategory category = detailedCategory(DETAILED_CATEGORY_ID, "FOOD_AND_DRINK_COFFEE");
        when(detailedCategoryRepository.findByCategoryCodeWithPrimaryCategory("FOOD_AND_DRINK_COFFEE"))
                .thenReturn(Optional.of(category));
        DetailedCategoryServiceImpl service = new DetailedCategoryServiceImpl(detailedCategoryRepository);

        var first = service.getByCategoryCode("FOOD_AND_DRINK_COFFEE");
        var second = service.getByCategoryCode("FOOD_AND_DRINK_COFFEE");

        assertThat(first).isSameAs(category);
        assertThat(second).isSameAs(category);
        verify(detailedCategoryRepository).findByCategoryCodeWithPrimaryCategory("FOOD_AND_DRINK_COFFEE");
    }

    @Test
    void getByCategoryCode_whenCategoryIsMissing_shouldReturnFallbackCategory() {
        TransactionDetailedCategory fallback = detailedCategory(FALLBACK_CATEGORY_ID, "OTHER_OTHER");
        when(detailedCategoryRepository.findByCategoryCodeWithPrimaryCategory("UNKNOWN_CATEGORY"))
                .thenReturn(Optional.empty());
        when(detailedCategoryRepository.findByCategoryCodeWithPrimaryCategory("OTHER_OTHER"))
                .thenReturn(Optional.of(fallback));

        var response = new DetailedCategoryServiceImpl(detailedCategoryRepository).getByCategoryCode("UNKNOWN_CATEGORY");

        assertThat(response).isSameAs(fallback);
    }

    @Test
    void getByCategoryCode_whenCategoryAndFallbackAreMissing_shouldThrowConflict() {
        when(detailedCategoryRepository.findByCategoryCodeWithPrimaryCategory("UNKNOWN_CATEGORY"))
                .thenReturn(Optional.empty());
        when(detailedCategoryRepository.findByCategoryCodeWithPrimaryCategory("OTHER_OTHER"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DetailedCategoryServiceImpl(detailedCategoryRepository).getByCategoryCode("UNKNOWN_CATEGORY"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("Default detailed category OTHER_OTHER is missing");
                });
    }

    @Test
    void getById_whenCategoryExists_shouldReturnCategory() {
        TransactionDetailedCategory category = detailedCategory(DETAILED_CATEGORY_ID, "TRAVEL_FLIGHTS");
        when(detailedCategoryRepository.findById(DETAILED_CATEGORY_ID)).thenReturn(Optional.of(category));

        var response = new DetailedCategoryServiceImpl(detailedCategoryRepository).getById(DETAILED_CATEGORY_ID);

        assertThat(response).isSameAs(category);
        verify(detailedCategoryRepository, never()).findByCategoryCodeWithPrimaryCategory("OTHER_OTHER");
    }

    @Test
    void getById_whenCategoryIsMissing_shouldThrowNotFound() {
        when(detailedCategoryRepository.findById(DETAILED_CATEGORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DetailedCategoryServiceImpl(detailedCategoryRepository).getById(DETAILED_CATEGORY_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Detailed category with id " + DETAILED_CATEGORY_ID + " not found");
                });
    }

    private TransactionDetailedCategory detailedCategory(UUID id, String code) {
        TransactionPrimaryCategory primaryCategory = new TransactionPrimaryCategory("PRIMARY", "Primary", "primary.svg");
        TransactionDetailedCategory category = new TransactionDetailedCategory("Display", code, primaryCategory, "Description");
        ReflectionTestUtils.setField(category, "transactionDetailedCategoryId", id);
        return category;
    }
}
