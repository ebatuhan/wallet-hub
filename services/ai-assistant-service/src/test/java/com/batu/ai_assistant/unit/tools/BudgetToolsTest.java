package com.batu.ai_assistant.unit.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.batu.ai_assistant.client.BudgetingClient;
import com.batu.ai_assistant.client.DashboardClient;
import com.batu.ai_assistant.client.TransactionCategoryClient;
import com.batu.ai_assistant.dto.client.CreateBudgetRequestDto;
import com.batu.ai_assistant.tools.BudgetTools;
import com.batu.shared.dto.response.BudgetResponseDto;
import com.batu.shared.dto.response.CursorResponse;
import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;

@ExtendWith(MockitoExtension.class)
class BudgetToolsTest {

    private static final UUID BUDGET_ID = UUID.fromString("92000000-0000-0000-0000-000000000001");
    private static final UUID CATEGORY_ID = UUID.fromString("92000000-0000-0000-0000-000000000002");

    @Mock
    private BudgetingClient budgetingClient;

    @Mock
    private DashboardClient dashboardClient;

    @Mock
    private TransactionCategoryClient transactionCategoryClient;

    @Test
    void getBudgets_whenCalled_shouldDelegatePaginationAndWrapResponse() {
        CursorResponse<BudgetResponseDto> budgets = new CursorResponse<>(List.of(budget()), true, "next-cursor");
        when(dashboardClient.getBudgets(10, "cursor-1", null, null)).thenReturn(ResponseEntity.ok(budgets));

        var response = tools().getBudgets(10, "cursor-1");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Active enriched budgets loaded.");
        assertThat(response.data()).isSameAs(budgets);
        verify(dashboardClient).getBudgets(10, "cursor-1", null, null);
    }

    @Test
    void getBudgets_whenDownstreamBodyIsNull_shouldReturnFailureInsteadOfSuccessfulNullData() {
        when(dashboardClient.getBudgets(null, null, null, null)).thenReturn(ResponseEntity.ok(null));

        var response = tools().getBudgets(null, null);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Budgets unavailable");
        assertThat(response.data()).isNull();
    }

    @Test
    void createBudget_whenCategoryMatchesIgnoringCaseAndOptionalFieldsAreMissing_shouldDefaultPeriodAndCurrentMonthStart() {
        when(transactionCategoryClient.getAllPrimaryCategories()).thenReturn(ResponseEntity.ok(categories()));
        when(budgetingClient.createBudget(any(CreateBudgetRequestDto.class))).thenReturn(ResponseEntity.ok(budget()));
        ArgumentCaptor<CreateBudgetRequestDto> requestCaptor = ArgumentCaptor.forClass(CreateBudgetRequestDto.class);

        var response = tools().createBudget(" food_and_drink ", decimal("250.00"), "USD", null, null);

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Budget created.");
        verify(budgetingClient).createBudget(requestCaptor.capture());
        assertThat(requestCaptor.getValue().categoryId()).isEqualTo(CATEGORY_ID);
        assertThat(requestCaptor.getValue().limitAmount()).isEqualByComparingTo("250.00");
        assertThat(requestCaptor.getValue().isoCurrencyCode()).isEqualTo("USD");
        assertThat(requestCaptor.getValue().period()).isEqualTo("MONTHLY");
        assertThat(requestCaptor.getValue().periodStart()).isEqualTo(LocalDate.now().withDayOfMonth(1));
    }

    @Test
    void createBudget_whenCategoryLookupBodyIsNull_shouldReturnFailureInsteadOfThrowingNullPointerException() {
        when(transactionCategoryClient.getAllPrimaryCategories()).thenReturn(ResponseEntity.ok(null));

        var response = tools().createBudget("FOOD_AND_DRINK", decimal("250.00"), "USD", "MONTHLY", "2026-05-01");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Primary categories unavailable");
        assertThat(response.data()).isNull();
        verify(budgetingClient, never()).createBudget(any());
    }

    @Test
    void createBudget_whenBudgetingBodyIsNull_shouldReturnFailureAndNotClaimBudgetWasCreated() {
        when(transactionCategoryClient.getAllPrimaryCategories()).thenReturn(ResponseEntity.ok(categories()));
        when(budgetingClient.createBudget(any(CreateBudgetRequestDto.class))).thenReturn(ResponseEntity.ok(null));

        var response = tools().createBudget("FOOD_AND_DRINK", decimal("250.00"), "USD", "MONTHLY", "2026-05-01");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Budget was not created");
        assertThat(response.data()).isNull();
    }

    @Test
    void createBudget_whenCategoryCodeIsUnknown_shouldReturnFailureAndNotCallBudgeting() {
        when(transactionCategoryClient.getAllPrimaryCategories()).thenReturn(ResponseEntity.ok(categories()));

        var response = tools().createBudget("TRAVEL", decimal("250.00"), "USD", "MONTHLY", "2026-05-01");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Unknown budget category code");
        assertThat(response.data()).isNull();
        verify(budgetingClient, never()).createBudget(any());
    }

    @Test
    void createBudget_whenPeriodStartIsInvalid_shouldReturnFailureAndNotCallBudgeting() {
        when(transactionCategoryClient.getAllPrimaryCategories()).thenReturn(ResponseEntity.ok(categories()));

        var response = tools().createBudget("FOOD_AND_DRINK", decimal("250.00"), "USD", "WEEKLY", "05/01/2026");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("periodStart must use yyyy-MM-dd");
        verify(budgetingClient, never()).createBudget(any());
    }

    @Test
    void updateBudget_whenInputsAreValid_shouldParseBudgetIdResolveCategoryAndDelegate() {
        when(transactionCategoryClient.getAllPrimaryCategories()).thenReturn(ResponseEntity.ok(categories()));
        when(budgetingClient.updateBudget(eq(BUDGET_ID), any(CreateBudgetRequestDto.class))).thenReturn(ResponseEntity.ok(budget()));
        ArgumentCaptor<CreateBudgetRequestDto> requestCaptor = ArgumentCaptor.forClass(CreateBudgetRequestDto.class);

        var response = tools().updateBudget(BUDGET_ID.toString(), "FOOD_AND_DRINK", decimal("300.00"), "EUR", "WEEKLY", "2026-05-04");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Budget updated.");
        verify(budgetingClient).updateBudget(eq(BUDGET_ID), requestCaptor.capture());
        assertThat(requestCaptor.getValue().categoryId()).isEqualTo(CATEGORY_ID);
        assertThat(requestCaptor.getValue().period()).isEqualTo("WEEKLY");
        assertThat(requestCaptor.getValue().periodStart()).isEqualTo(LocalDate.of(2026, 5, 4));
    }

    @Test
    void updateBudget_whenBudgetingBodyIsNull_shouldReturnFailureAndNotClaimBudgetWasUpdated() {
        when(transactionCategoryClient.getAllPrimaryCategories()).thenReturn(ResponseEntity.ok(categories()));
        when(budgetingClient.updateBudget(eq(BUDGET_ID), any(CreateBudgetRequestDto.class))).thenReturn(ResponseEntity.ok(null));

        var response = tools().updateBudget(BUDGET_ID.toString(), "FOOD_AND_DRINK", decimal("300.00"), "USD", null, null);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Budget was not updated");
        assertThat(response.data()).isNull();
    }

    @Test
    void updateBudget_whenBudgetIdIsInvalid_shouldReturnFailureBeforeCategoryLookup() {
        var response = tools().updateBudget("not-a-uuid", "FOOD_AND_DRINK", decimal("300.00"), "USD", null, null);

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Invalid budget ID");
        verify(transactionCategoryClient, never()).getAllPrimaryCategories();
        verify(budgetingClient, never()).updateBudget(any(), any());
    }

    @Test
    void deactivateBudget_whenBudgetIdIsValid_shouldDelegateDelete() {
        var response = tools().deactivateBudget(" " + BUDGET_ID + " ");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("Budget deactivated.");
        assertThat(response.data()).isNull();
        verify(budgetingClient).deactivateBudget(BUDGET_ID);
    }

    @Test
    void deactivateBudget_whenBudgetIdIsInvalid_shouldReturnFailureAndNotCallBudgeting() {
        var response = tools().deactivateBudget("bad-id");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("Invalid budget ID");
        verify(budgetingClient, never()).deactivateBudget(any());
    }

    private BudgetTools tools() {
        return new BudgetTools(budgetingClient, dashboardClient, transactionCategoryClient);
    }

    private static List<TransactionPrimaryCategoryDto> categories() {
        return List.of(new TransactionPrimaryCategoryDto(CATEGORY_ID, "FOOD_AND_DRINK", "Food", "food.svg"));
    }

    private static BudgetResponseDto budget() {
        return new BudgetResponseDto(
                BUDGET_ID,
                CATEGORY_ID,
                "FOOD_AND_DRINK",
                "Food",
                "food.svg",
                decimal("250.00"),
                decimal("42.00"),
                "USD",
                "MONTHLY",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                true);
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }
}
