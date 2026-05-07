package com.batu.ai_assistant.unit.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.batu.ai_assistant.client.TransactionCategoryClient;
import com.batu.ai_assistant.tools.LookupTools;
import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;

@ExtendWith(MockitoExtension.class)
class LookupToolsTest {

    @Mock
    private TransactionCategoryClient transactionCategoryClient;

    @Test
    void getAllPrimaryCategories_whenCalled_shouldDelegateAndWrapCategories() {
        List<TransactionPrimaryCategoryDto> categories = List.of(new TransactionPrimaryCategoryDto(
                UUID.fromString("94000000-0000-0000-0000-000000000001"),
                "FOOD_AND_DRINK",
                "Food",
                "food.svg"));
        when(transactionCategoryClient.getAllPrimaryCategories()).thenReturn(ResponseEntity.ok(categories));

        var response = new LookupTools(transactionCategoryClient).getAllPrimaryCategories();

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Primary categories loaded.");
        assertThat(response.data()).isSameAs(categories);
        verify(transactionCategoryClient).getAllPrimaryCategories();
    }

    @Test
    void getAllPrimaryCategories_whenDownstreamBodyIsNull_shouldReturnFailureInsteadOfSuccessfulNullData() {
        when(transactionCategoryClient.getAllPrimaryCategories()).thenReturn(ResponseEntity.ok(null));

        var response = new LookupTools(transactionCategoryClient).getAllPrimaryCategories();

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Primary categories unavailable");
        assertThat(response.data()).isNull();
    }

}
