package com.batu.budgeting.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.batu.budgeting.dto.BudgetResponse;
import com.batu.budgeting.dto.CreateBudgetRequest;
import com.batu.budgeting.entity.Budget;
import com.batu.budgeting.entity.BudgetPeriod;
import com.batu.budgeting.enums.BudgetSortField;
import com.batu.budgeting.mapper.BudgetMapper;
import com.batu.budgeting.repository.BudgetRepository;
import com.batu.budgeting.service.impl.BudgetServiceImpl;
import com.batu.shared.cursor.CursorUtils;
import com.batu.shared.messaging.event.TransactionRecorded;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    private static final UUID USER_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("50000000-0000-0000-0000-000000000002");
    private static final UUID BUDGET_ID = UUID.fromString("50000000-0000-0000-0000-000000000003");
    private static final UUID CATEGORY_ID = UUID.fromString("50000000-0000-0000-0000-000000000004");
    private static final UUID ACCOUNT_ID = UUID.fromString("50000000-0000-0000-0000-000000000005");
    private static final UUID TRANSACTION_ID = UUID.fromString("50000000-0000-0000-0000-000000000006");

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CursorUtils cursorUtils;

    private BudgetServiceImpl budgetService;

    @BeforeEach
    void setUp() {
        budgetService = new BudgetServiceImpl(budgetRepository, new BudgetMapper(), cursorUtils);
    }

    @Test
    void createBudget_whenRequestDoesNotOverlap_shouldSaveBudgetForUserAndReturnResponse() {
        CreateBudgetRequest request = monthlyRequest(LocalDate.of(2026, 5, 1));
        when(budgetRepository.findByUserIdAndCategoryIdAndIsoCurrencyCodeAndActiveTrue(USER_ID, CATEGORY_ID, "USD"))
                .thenReturn(List.of());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<Budget> budgetCaptor = ArgumentCaptor.forClass(Budget.class);

        BudgetResponse response = budgetService.createBudget(request, USER_ID);

        verify(budgetRepository).save(budgetCaptor.capture());
        Budget savedBudget = budgetCaptor.getValue();
        assertThat(savedBudget.getUserId()).isEqualTo(USER_ID);
        assertThat(savedBudget.getCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(savedBudget.getLimitAmount()).isEqualByComparingTo("500.00");
        assertThat(savedBudget.getSpentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(savedBudget.getIsoCurrencyCode()).isEqualTo("USD");
        assertThat(savedBudget.getPeriod()).isEqualTo(BudgetPeriod.MONTHLY);
        assertThat(savedBudget.getPeriodStart()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(savedBudget.isActive()).isTrue();
        assertThat(response.periodEnd()).isEqualTo(LocalDate.of(2026, 5, 31));
    }

    @Test
    void createBudget_whenActiveBudgetOverlaps_shouldThrowConflictAndNotSave() {
        CreateBudgetRequest request = monthlyRequest(LocalDate.of(2026, 5, 15));
        Budget existingBudget = budget(BUDGET_ID, USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        when(budgetRepository.findByUserIdAndCategoryIdAndIsoCurrencyCodeAndActiveTrue(USER_ID, CATEGORY_ID, "USD"))
                .thenReturn(List.of(existingBudget));

        assertThatThrownBy(() -> budgetService.createBudget(request, USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason()).isEqualTo("Overlapping active budget exists for this category and currency");
                });

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void createBudget_whenPeriodIsAdjacentAndNonOverlapping_shouldSaveBudget() {
        CreateBudgetRequest request = monthlyRequest(LocalDate.of(2026, 6, 1));
        Budget existingBudget = budget(BUDGET_ID, USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        when(budgetRepository.findByUserIdAndCategoryIdAndIsoCurrencyCodeAndActiveTrue(USER_ID, CATEGORY_ID, "USD"))
                .thenReturn(List.of(existingBudget));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetResponse response = budgetService.createBudget(request, USER_ID);

        verify(budgetRepository).save(any(Budget.class));
        assertThat(response.periodStart()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(response.periodEnd()).isEqualTo(LocalDate.of(2026, 6, 30));
    }

    @ParameterizedTest
    @CsvSource({
            "2026-05-01, 2026-05-31, true",
            "2026-05-15, 2026-05-01, true",
            "2026-04-15, 2026-05-01, true",
            "2026-05-31, 2026-05-01, true",
            "2026-06-01, 2026-05-01, false",
            "2026-04-01, 2026-05-01, false"
    })
    void createBudget_whenCheckingPeriodBoundaries_shouldRejectOnlyOverlappingPeriods(
            LocalDate requestStart,
            LocalDate existingStart,
            boolean overlaps) {
        CreateBudgetRequest request = monthlyRequest(requestStart);
        Budget existingBudget = budget(BUDGET_ID, USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, existingStart);
        when(budgetRepository.findByUserIdAndCategoryIdAndIsoCurrencyCodeAndActiveTrue(USER_ID, CATEGORY_ID, "USD"))
                .thenReturn(List.of(existingBudget));

        if (overlaps) {
            assertThatThrownBy(() -> budgetService.createBudget(request, USER_ID))
                    .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                            assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
            verify(budgetRepository, never()).save(any());
            return;
        }

        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BudgetResponse response = budgetService.createBudget(request, USER_ID);

        assertThat(response.periodStart()).isEqualTo(requestStart);
        verify(budgetRepository).save(any(Budget.class));
    }

    @Test
    void updateBudget_whenBudgetDoesNotExist_shouldThrowNotFound() {
        CreateBudgetRequest request = monthlyRequest(LocalDate.of(2026, 5, 1));
        when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.updateBudget(BUDGET_ID, request, USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Budget with id " + BUDGET_ID + " not found");
                });

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void updateBudget_whenBudgetBelongsToAnotherUser_shouldQueryByAuthenticatedUserAndThrowNotFound() {
        CreateBudgetRequest request = monthlyRequest(LocalDate.of(2026, 5, 1));
        when(budgetRepository.findByIdAndUserId(BUDGET_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.updateBudget(BUDGET_ID, request, OTHER_USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Budget with id " + BUDGET_ID + " not found");
                });

        verify(budgetRepository).findByIdAndUserId(BUDGET_ID, OTHER_USER_ID);
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void updateBudget_whenOnlySelfOverlaps_shouldUpdateBudget() {
        CreateBudgetRequest request = monthlyRequest(LocalDate.of(2026, 5, 10));
        Budget existingBudget = budget(BUDGET_ID, USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID)).thenReturn(Optional.of(existingBudget));
        when(budgetRepository.findByUserIdAndCategoryIdAndIsoCurrencyCodeAndActiveTrue(USER_ID, CATEGORY_ID, "USD"))
                .thenReturn(List.of(existingBudget));
        when(budgetRepository.save(existingBudget)).thenReturn(existingBudget);

        BudgetResponse response = budgetService.updateBudget(BUDGET_ID, request, USER_ID);

        assertThat(existingBudget.getPeriodStart()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(response.periodEnd()).isEqualTo(LocalDate.of(2026, 6, 9));
        verify(budgetRepository).save(existingBudget);
    }

    @Test
    void updateBudget_whenOtherBudgetOverlaps_shouldThrowConflictAndNotSave() {
        CreateBudgetRequest request = monthlyRequest(LocalDate.of(2026, 5, 10));
        Budget existingBudget = budget(BUDGET_ID, USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 4, 1));
        Budget otherBudget = budget(UUID.fromString("50000000-0000-0000-0000-000000000007"), USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID)).thenReturn(Optional.of(existingBudget));
        when(budgetRepository.findByUserIdAndCategoryIdAndIsoCurrencyCodeAndActiveTrue(USER_ID, CATEGORY_ID, "USD"))
                .thenReturn(List.of(existingBudget, otherBudget));

        assertThatThrownBy(() -> budgetService.updateBudget(BUDGET_ID, request, USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void getBudgets_whenCursorMissingAndWindowHasNoNextPage_shouldUseInitialKeysetAndReturnNullCursor() {
        Budget budget = budget(BUDGET_ID, USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        mockWindow(Window.from(List.of(budget), index -> ScrollPosition.keyset(), false));

        var response = budgetService.getBudgets(USER_ID, null, 10, null, Sort.Direction.DESC);

        assertThat(response.getData()).singleElement().satisfies(budgetResponse ->
                assertThat(budgetResponse.id()).isEqualTo(BUDGET_ID));
        assertThat(response.isHasMore()).isFalse();
        assertThat(response.getNextCursor()).isNull();
        org.mockito.Mockito.verifyNoInteractions(cursorUtils);
    }

    @Test
    void getBudgets_whenCursorPresentAndWindowHasNextPage_shouldDecodeCursorAndReturnEncodedNextCursor() {
        Budget budget = budget(BUDGET_ID, USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        when(cursorUtils.decode("cursor-1")).thenReturn(ScrollPosition.keyset());
        when(cursorUtils.encode(any(ScrollPosition.class))).thenReturn("cursor-2");
        mockWindow(Window.from(List.of(budget), index -> ScrollPosition.forward(java.util.Map.of("id", BUDGET_ID)), true));

        var response = budgetService.getBudgets(USER_ID, "cursor-1", 20, BudgetSortField.SPENT_AMOUNT, Sort.Direction.ASC);

        assertThat(response.isHasMore()).isTrue();
        assertThat(response.getNextCursor()).isEqualTo("cursor-2");
        verify(cursorUtils).decode("cursor-1");
        verify(cursorUtils).encode(any(ScrollPosition.class));
    }

    @Test
    void deactivateBudget_whenBudgetExists_shouldMarkInactiveAndSave() {
        Budget existingBudget = budget(BUDGET_ID, USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID)).thenReturn(Optional.of(existingBudget));

        budgetService.deactivateBudget(BUDGET_ID, USER_ID);

        assertThat(existingBudget.isActive()).isFalse();
        verify(budgetRepository).save(existingBudget);
    }

    @Test
    void deactivateBudget_whenBudgetDoesNotExist_shouldThrowNotFound() {
        when(budgetRepository.findByIdAndUserId(BUDGET_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.deactivateBudget(BUDGET_ID, USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Budget with id " + BUDGET_ID + " not found");
                });

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void deactivateBudget_whenBudgetBelongsToAnotherUser_shouldQueryByAuthenticatedUserAndThrowNotFound() {
        when(budgetRepository.findByIdAndUserId(BUDGET_ID, OTHER_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> budgetService.deactivateBudget(BUDGET_ID, OTHER_USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Budget with id " + BUDGET_ID + " not found");
                });

        verify(budgetRepository).findByIdAndUserId(BUDGET_ID, OTHER_USER_ID);
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void applyTransaction_whenMatchingExpense_shouldIncreaseSpentAmountByAbsoluteAmount() {
        Budget budget = budget(BUDGET_ID, USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        budget.setSpentAmount(new BigDecimal("10.00"));
        when(budgetRepository.findCandidates(USER_ID, CATEGORY_ID, "USD", LocalDate.of(2026, 5, 6)))
                .thenReturn(List.of(budget));

        budgetService.applyTransaction(transaction(new BigDecimal("-25.50"), true, false, CATEGORY_ID, "USD", LocalDate.of(2026, 5, 6)));

        assertThat(budget.getSpentAmount()).isEqualByComparingTo("35.50");
        verify(budgetRepository).save(budget);
    }

    @Test
    void applyTransaction_whenTransactionIsInactive_shouldDoNothing() {
        budgetService.applyTransaction(transaction(new BigDecimal("-25.50"), false, false, CATEGORY_ID, "USD", LocalDate.of(2026, 5, 6)));

        verify(budgetRepository, never()).findCandidates(any(), any(), any(), any());
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void applyTransaction_whenTransactionIsPending_shouldDoNothing() {
        budgetService.applyTransaction(transaction(new BigDecimal("-25.50"), true, true, CATEGORY_ID, "USD", LocalDate.of(2026, 5, 6)));

        verify(budgetRepository, never()).findCandidates(any(), any(), any(), any());
        verify(budgetRepository, never()).save(any());
    }

    @ParameterizedTest
    @CsvSource({ "0.00", "25.50" })
    void applyTransaction_whenAmountIsNotExpense_shouldDoNothing(BigDecimal amount) {
        budgetService.applyTransaction(transaction(amount, true, false, CATEGORY_ID, "USD", LocalDate.of(2026, 5, 6)));

        verify(budgetRepository, never()).findCandidates(any(), any(), any(), any());
        verify(budgetRepository, never()).save(any());
    }

    @ParameterizedTest
    @MethodSource("transactionsMissingRequiredFields")
    void applyTransaction_whenRequiredFieldsAreMissing_shouldDoNothing(TransactionRecorded transaction) {
        budgetService.applyTransaction(transaction);

        verify(budgetRepository, never()).findCandidates(any(), any(), any(), any());
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void applyTransaction_whenPendingIsNullAndTransactionMatches_shouldApplyTransaction() {
        Budget budget = budget(BUDGET_ID, USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        when(budgetRepository.findCandidates(USER_ID, CATEGORY_ID, "USD", LocalDate.of(2026, 5, 6)))
                .thenReturn(List.of(budget));

        budgetService.applyTransaction(transaction(new BigDecimal("-25.50"), true, null, CATEGORY_ID, "USD", LocalDate.of(2026, 5, 6)));

        assertThat(budget.getSpentAmount()).isEqualByComparingTo("25.50");
        verify(budgetRepository).save(budget);
    }

    @Test
    void applyTransaction_whenNoCandidateExists_shouldDoNothing() {
        when(budgetRepository.findCandidates(USER_ID, CATEGORY_ID, "USD", LocalDate.of(2026, 5, 6)))
                .thenReturn(List.of());

        budgetService.applyTransaction(transaction(new BigDecimal("-25.50"), true, false, CATEGORY_ID, "USD", LocalDate.of(2026, 5, 6)));

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void applyTransaction_whenCandidatePeriodEndedBeforeTransactionDate_shouldDoNothing() {
        Budget budget = budget(BUDGET_ID, USER_ID, CATEGORY_ID, "USD", BudgetPeriod.WEEKLY, LocalDate.of(2026, 5, 1));
        when(budgetRepository.findCandidates(USER_ID, CATEGORY_ID, "USD", LocalDate.of(2026, 5, 20)))
                .thenReturn(List.of(budget));

        budgetService.applyTransaction(transaction(new BigDecimal("-25.50"), true, false, CATEGORY_ID, "USD", LocalDate.of(2026, 5, 20)));

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void applyTransaction_whenMultipleCandidatesMatch_shouldApplyFirstCandidateOnly() {
        Budget firstBudget = budget(BUDGET_ID, USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        Budget secondBudget = budget(UUID.fromString("50000000-0000-0000-0000-000000000008"), USER_ID, CATEGORY_ID, "USD", BudgetPeriod.MONTHLY, LocalDate.of(2026, 5, 1));
        when(budgetRepository.findCandidates(USER_ID, CATEGORY_ID, "USD", LocalDate.of(2026, 5, 6)))
                .thenReturn(List.of(firstBudget, secondBudget));

        budgetService.applyTransaction(transaction(new BigDecimal("-25.50"), true, false, CATEGORY_ID, "USD", LocalDate.of(2026, 5, 6)));

        assertThat(firstBudget.getSpentAmount()).isEqualByComparingTo("25.50");
        assertThat(secondBudget.getSpentAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(budgetRepository).save(firstBudget);
        verify(budgetRepository, never()).save(secondBudget);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void mockWindow(Window<Budget> window) {
        org.mockito.Mockito.doReturn(window).when(budgetRepository).findBy(
                any(Specification.class),
                any(Function.class));
    }

    private static Stream<Arguments> transactionsMissingRequiredFields() {
        UUID categoryId = CATEGORY_ID;
        LocalDate date = LocalDate.of(2026, 5, 6);
        return Stream.of(
                Arguments.of(transactionWith(new BigDecimal("-25.50"), categoryId, "USD", null)),
                Arguments.of(transactionWith(new BigDecimal("-25.50"), categoryId, null, date)),
                Arguments.of(transactionWith(new BigDecimal("-25.50"), null, "USD", date)),
                Arguments.of(transactionWith(null, categoryId, "USD", date)));
    }

    private static TransactionRecorded transactionWith(BigDecimal amount, UUID categoryId, String currency, LocalDate date) {
        return new TransactionRecorded(
                TRANSACTION_ID,
                USER_ID,
                ACCOUNT_ID,
                amount,
                currency,
                "Coffee",
                "PLACE",
                date,
                false,
                "in store",
                categoryId,
                "FOOD_AND_DRINK",
                true);
    }

    private CreateBudgetRequest monthlyRequest(LocalDate periodStart) {
        return new CreateBudgetRequest(
                CATEGORY_ID,
                new BigDecimal("500.00"),
                "USD",
                BudgetPeriod.MONTHLY,
                periodStart);
    }

    private Budget budget(UUID budgetId, UUID userId, UUID categoryId, String currency, BudgetPeriod period, LocalDate periodStart) {
        Budget budget = new Budget(userId, categoryId, new BigDecimal("500.00"), currency, period, periodStart);
        ReflectionTestUtils.setField(budget, "id", budgetId);
        return budget;
    }

    private TransactionRecorded transaction(BigDecimal amount, boolean active, Boolean pending, UUID categoryId,
            String currency, LocalDate date) {
        return new TransactionRecorded(
                TRANSACTION_ID,
                USER_ID,
                ACCOUNT_ID,
                amount,
                currency,
                "Coffee",
                "PLACE",
                date,
                pending,
                "in store",
                categoryId,
                "FOOD_AND_DRINK",
                active);
    }
}
