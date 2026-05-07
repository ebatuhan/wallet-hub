package com.batu.transaction_service.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
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
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Window;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.batu.shared.cursor.CursorUtils;
import com.batu.shared.dto.request.TransactionUpsertRequestDto;
import com.batu.shared.dto.response.TransactionDetailedCategoryDto;
import com.batu.shared.dto.response.TransactionDto;
import com.batu.shared.dto.response.TransactionPrimaryCategoryDto;
import com.batu.shared.messaging.event.TransactionRecorded;
import com.batu.shared.messaging.event.TransactionRemoved;
import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.entity.TransactionDetailedCategory;
import com.batu.transaction_service.entity.TransactionPrimaryCategory;
import com.batu.transaction_service.mapper.TransactionSyncMapper;
import com.batu.transaction_service.messaging.OutboxDomainEventPublisher;
import com.batu.transaction_service.repository.TransactionRepository;
import com.batu.transaction_service.service.DetailedCategoryService;
import com.batu.transaction_service.service.impl.TransactionServiceImpl;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    private static final UUID TRANSACTION_ID = UUID.fromString("e1000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_TRANSACTION_ID = UUID.fromString("e1000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID = UUID.fromString("e1000000-0000-0000-0000-000000000003");
    private static final UUID OTHER_USER_ID = UUID.fromString("e1000000-0000-0000-0000-000000000004");
    private static final UUID ACCOUNT_ID = UUID.fromString("e1000000-0000-0000-0000-000000000005");
    private static final UUID PRIMARY_CATEGORY_ID = UUID.fromString("e1000000-0000-0000-0000-000000000006");
    private static final UUID DETAILED_CATEGORY_ID = UUID.fromString("e1000000-0000-0000-0000-000000000007");

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CursorUtils cursorUtils;

    @Mock
    private DetailedCategoryService detailedCategoryService;

    @Mock
    private TransactionSyncMapper transactionSyncMapper;

    @Mock
    private OutboxDomainEventPublisher eventPublisher;

    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(
                transactionRepository,
                cursorUtils,
                detailedCategoryService,
                transactionSyncMapper,
                eventPublisher);
    }

    @Test
    void transactions_whenCursorMissingAndWindowHasNoNext_shouldReturnMappedTransactionsWithoutNextCursor() {
        Transaction transaction = transaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, true, false);
        mockWindow(Window.from(List.of(transaction), index -> ScrollPosition.keyset(), false));

        var response = transactionService.transactions(jwt(USER_ID), null, null, null, 10);

        assertThat(response.isHasMore()).isFalse();
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.getData()).singleElement().satisfies(view -> {
            assertThat(view.getTransactionId()).isEqualTo(TRANSACTION_ID);
            assertThat(view.getAmount()).isEqualByComparingTo("42.50");
            assertThat(view.getTransactionName()).isEqualTo("Coffee Shop");
            assertThat(view.getPrimaryCategoryId()).isEqualTo(PRIMARY_CATEGORY_ID);
            assertThat(view.getDetailedCategoryId()).isEqualTo(DETAILED_CATEGORY_ID);
            assertThat(view.getAccountId()).isEqualTo(ACCOUNT_ID);
        });
        verify(cursorUtils, never()).decode(any());
        verify(cursorUtils, never()).encode(any());
    }

    @Test
    void transactions_whenCursorProvidedAndWindowHasNext_shouldDecodeAndReturnNextCursor() {
        Transaction transaction = transaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, true, false);
        ScrollPosition decodedPosition = ScrollPosition.keyset();
        when(cursorUtils.decode("cursor-1")).thenReturn(decodedPosition);
        when(cursorUtils.encode(any(ScrollPosition.class))).thenReturn("cursor-2");
        mockWindow(Window.from(List.of(transaction), index -> ScrollPosition.forward(java.util.Map.of("transactionId", TRANSACTION_ID)), true));

        var response = transactionService.transactions(USER_ID, "FOOD_AND_DRINK", ACCOUNT_ID, "cursor-1", 20);

        assertThat(response.isHasMore()).isTrue();
        assertThat(response.getNextCursor()).isEqualTo("cursor-2");
        verify(cursorUtils).decode("cursor-1");
        verify(cursorUtils).encode(any(ScrollPosition.class));
    }

    @Test
    void transactions_whenRepositoryReturnsEmptyWindow_shouldReturnEmptyCursorResponse() {
        mockWindow(Window.from(List.of(), index -> ScrollPosition.keyset(), false));

        var response = transactionService.transactions(USER_ID, "", null, "", 1);

        assertThat(response.getData()).isEmpty();
        assertThat(response.isHasMore()).isFalse();
        assertThat(response.getNextCursor()).isNull();
        verify(cursorUtils, never()).decode(any());
        verify(cursorUtils, never()).encode(any());
    }

    @Test
    void getTransactionById_whenTransactionIsOwnedAndActive_shouldReturnMappedTransaction() {
        Transaction transaction = transaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, true, false);
        TransactionDto dto = transactionDto(transaction);
        when(transactionRepository.findByTransactionIdAndUserIdWithCategory(TRANSACTION_ID, USER_ID))
                .thenReturn(Optional.of(transaction));
        when(transactionSyncMapper.toDto(transaction)).thenReturn(dto);

        var response = transactionService.getTransactionById(jwt(USER_ID), TRANSACTION_ID);

        assertThat(response).isSameAs(dto);
        verify(transactionSyncMapper).toDto(transaction);
    }

    @ParameterizedTest
    @MethodSource("inaccessibleTransactionCases")
    void getTransactionById_whenTransactionIsMissingInactiveOrForeign_shouldThrowNotFound(
            String caseName,
            UUID transactionId,
            UUID userId,
            Optional<Transaction> repositoryResult) {
        when(transactionRepository.findByTransactionIdAndUserIdWithCategory(transactionId, userId))
                .thenReturn(repositoryResult);

        assertThatThrownBy(() -> transactionService.getTransactionById(userId, transactionId))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Transaction with id " + transactionId + " not found");
                });
        verify(transactionSyncMapper, never()).toDto(any());
    }

    @ParameterizedTest
    @ValueSource(strings = { "not-a-uuid", "", "e1000000-0000-0000-0000-not-a-uuid" })
    void getTransactionById_whenJwtSubjectIsMalformed_shouldThrowIllegalArgumentExceptionAndNotQueryRepository(String subject) {
        Jwt principal = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(subject)
                .build();

        assertThatThrownBy(() -> transactionService.getTransactionById(principal, TRANSACTION_ID))
                .isInstanceOf(IllegalArgumentException.class);
        verify(transactionRepository, never()).findByTransactionIdAndUserIdWithCategory(any(), any());
    }

    @Test
    void upsertTransaction_whenRequestIsActive_shouldPublishRecordedEventAndReturnResponse() {
        TransactionUpsertRequestDto request = upsertRequest(TRANSACTION_ID, true, "Coffee Shop");
        TransactionDetailedCategory category = detailedCategory();
        Transaction savedTransaction = transaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, true, false);
        when(detailedCategoryService.getByCategoryCode("FOOD_AND_DRINK_COFFEE")).thenReturn(category);
        when(transactionRepository.upsertTransaction(request, DETAILED_CATEGORY_ID)).thenReturn(savedTransaction);
        ArgumentCaptor<TransactionRecorded> eventCaptor = ArgumentCaptor.forClass(TransactionRecorded.class);

        var response = transactionService.upsertTransaction(request);

        verify(eventPublisher).publishTransactionRecorded(eventCaptor.capture());
        TransactionRecorded event = eventCaptor.getValue();
        assertThat(event.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(event.getUserId()).isEqualTo(USER_ID);
        assertThat(event.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(event.getPrimaryCategoryId()).isEqualTo(PRIMARY_CATEGORY_ID);
        assertThat(event.isActive()).isTrue();
        verify(eventPublisher, never()).publishTransactionRemoved(any(TransactionRemoved.class));
        assertThat(response.getTransactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(response.getDetailedCategoryCode()).isEqualTo("FOOD_AND_DRINK_COFFEE");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void upsertTransaction_whenRequestDeactivatesTransaction_shouldPublishRemovedEventAndReturnInactiveResponse() {
        TransactionUpsertRequestDto request = upsertRequest(TRANSACTION_ID, false, "Removed Coffee");
        TransactionDetailedCategory category = detailedCategory();
        Transaction savedTransaction = transaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, false, false);
        when(detailedCategoryService.getByCategoryCode("FOOD_AND_DRINK_COFFEE")).thenReturn(category);
        when(transactionRepository.upsertTransaction(request, DETAILED_CATEGORY_ID)).thenReturn(savedTransaction);
        ArgumentCaptor<TransactionRemoved> eventCaptor = ArgumentCaptor.forClass(TransactionRemoved.class);

        var response = transactionService.upsertTransaction(request);

        verify(eventPublisher).publishTransactionRemoved(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getTransactionId()).isEqualTo(TRANSACTION_ID);
        verify(eventPublisher, never()).publishTransactionRecorded(any(TransactionRecorded.class));
        assertThat(response.isActive()).isFalse();
    }

    @Test
    void upsertTransaction_whenCategoryLookupFails_shouldNotPersistOrPublishEvent() {
        TransactionUpsertRequestDto request = upsertRequest(TRANSACTION_ID, true, "Coffee Shop");
        ResponseStatusException failure = new ResponseStatusException(HttpStatus.NOT_FOUND, "category missing");
        when(detailedCategoryService.getByCategoryCode("FOOD_AND_DRINK_COFFEE")).thenThrow(failure);

        assertThatThrownBy(() -> transactionService.upsertTransaction(request)).isSameAs(failure);
        verify(transactionRepository, never()).upsertTransaction(any(), any());
        verify(eventPublisher, never()).publishTransactionRecorded(any());
        verify(eventPublisher, never()).publishTransactionRemoved(any());
    }

    @Test
    void upsertTransaction_whenRepositoryFails_shouldNotPublishEvent() {
        TransactionUpsertRequestDto request = upsertRequest(TRANSACTION_ID, true, "Coffee Shop");
        TransactionDetailedCategory category = detailedCategory();
        RuntimeException failure = new RuntimeException("database unavailable");
        when(detailedCategoryService.getByCategoryCode("FOOD_AND_DRINK_COFFEE")).thenReturn(category);
        when(transactionRepository.upsertTransaction(request, DETAILED_CATEGORY_ID)).thenThrow(failure);

        assertThatThrownBy(() -> transactionService.upsertTransaction(request)).isSameAs(failure);
        verify(eventPublisher, never()).publishTransactionRecorded(any());
        verify(eventPublisher, never()).publishTransactionRemoved(any());
    }

    @Test
    void deactivateTransactionsByAccountId_whenActiveTransactionsExist_shouldDeactivateSaveAndPublishRemovedEvents() {
        Transaction coffee = transaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, true, false);
        Transaction groceries = transaction(OTHER_TRANSACTION_ID, USER_ID, ACCOUNT_ID, true, true);
        when(transactionRepository.findByAccountIdAndIsActiveTrue(ACCOUNT_ID)).thenReturn(List.of(coffee, groceries));
        when(transactionRepository.saveAll(List.of(coffee, groceries))).thenReturn(List.of(coffee, groceries));
        ArgumentCaptor<TransactionRemoved> eventCaptor = ArgumentCaptor.forClass(TransactionRemoved.class);

        var response = transactionService.deactivateTransactionsByAccountId(ACCOUNT_ID);

        assertThat(coffee.isActive()).isFalse();
        assertThat(groceries.isActive()).isFalse();
        assertThat(response).hasSize(2).allSatisfy(transaction -> assertThat(transaction.isActive()).isFalse());
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishTransactionRemoved(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).extracting(TransactionRemoved::getTransactionId)
                .containsExactly(TRANSACTION_ID, OTHER_TRANSACTION_ID);
    }

    @Test
    void deactivateTransactionsByAccountId_whenNoActiveTransactionsExist_shouldReturnEmptyListAndNotPublishEvents() {
        when(transactionRepository.findByAccountIdAndIsActiveTrue(ACCOUNT_ID)).thenReturn(List.of());
        when(transactionRepository.saveAll(List.of())).thenReturn(List.of());

        var response = transactionService.deactivateTransactionsByAccountId(ACCOUNT_ID);

        assertThat(response).isEmpty();
        verify(eventPublisher, never()).publishTransactionRemoved(any());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void mockWindow(Window<Transaction> window) {
        doReturn(window).when(transactionRepository).findBy(any(Specification.class), any(Function.class));
    }

    private static Stream<Arguments> inaccessibleTransactionCases() {
        return Stream.of(
                Arguments.of("missing", TRANSACTION_ID, USER_ID, Optional.empty()),
                Arguments.of("inactive", TRANSACTION_ID, USER_ID, Optional.of(transaction(TRANSACTION_ID, USER_ID, ACCOUNT_ID, false, false))),
                Arguments.of("foreign", TRANSACTION_ID, OTHER_USER_ID, Optional.empty()));
    }

    private Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(userId.toString())
                .build();
    }

    private static TransactionUpsertRequestDto upsertRequest(UUID transactionId, boolean active, String transactionName) {
        return new TransactionUpsertRequestDto(
                transactionId,
                USER_ID,
                ACCOUNT_ID,
                new BigDecimal("42.50"),
                "USD",
                transactionName,
                "place",
                LocalDate.of(2026, 5, 7),
                false,
                "in store",
                "FOOD_AND_DRINK_COFFEE",
                active);
    }

    private static Transaction transaction(UUID transactionId, UUID userId, UUID accountId, boolean active, boolean pending) {
        return new Transaction(
                transactionId,
                userId,
                accountId,
                new BigDecimal("42.50"),
                "USD",
                "Coffee Shop",
                "place",
                LocalDate.of(2026, 5, 7),
                pending,
                "in store",
                detailedCategory(),
                active);
    }

    private static TransactionDetailedCategory detailedCategory() {
        TransactionPrimaryCategory primaryCategory = new TransactionPrimaryCategory("FOOD_AND_DRINK", "Food & Drink", "food.svg");
        ReflectionTestUtils.setField(primaryCategory, "transactionPrimaryCategoryId", PRIMARY_CATEGORY_ID);
        TransactionDetailedCategory detailedCategory = new TransactionDetailedCategory(
                "Coffee",
                "FOOD_AND_DRINK_COFFEE",
                primaryCategory,
                "Coffee shops");
        ReflectionTestUtils.setField(detailedCategory, "transactionDetailedCategoryId", DETAILED_CATEGORY_ID);
        return detailedCategory;
    }

    private TransactionDto transactionDto(Transaction transaction) {
        TransactionPrimaryCategoryDto primaryCategory = new TransactionPrimaryCategoryDto(
                PRIMARY_CATEGORY_ID,
                "FOOD_AND_DRINK",
                "Food & Drink",
                "food.svg");
        TransactionDetailedCategoryDto detailedCategory = new TransactionDetailedCategoryDto(
                DETAILED_CATEGORY_ID,
                "Coffee",
                "FOOD_AND_DRINK_COFFEE",
                primaryCategory);
        return new TransactionDto(
                transaction.getTransactionId(),
                transaction.getUserId(),
                transaction.getAccountId(),
                transaction.getAmount(),
                transaction.getIsoCurrencyCode(),
                transaction.getTransactionName(),
                transaction.getTransactionType(),
                transaction.getDate(),
                transaction.getPending(),
                transaction.getPaymentChannel(),
                detailedCategory,
                transaction.getCreatedAt(),
                transaction.getUpdatedAt());
    }
}
