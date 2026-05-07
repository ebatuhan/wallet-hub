package com.batu.insights_service.unit.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.batu.insights_service.entity.TransactionInsightRow;
import com.batu.insights_service.messaging.InsightsInbox;
import com.batu.insights_service.messaging.TransactionEventListener;
import com.batu.insights_service.service.TransactionInsightsService;
import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.event.TransactionRecorded;

@ExtendWith(MockitoExtension.class)
class TransactionEventListenerTest {

    private static final UUID EVENT_ID = UUID.fromString("86000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("86000000-0000-0000-0000-000000000002");
    private static final UUID ACCOUNT_ID = UUID.fromString("86000000-0000-0000-0000-000000000003");
    private static final UUID TRANSACTION_ID = UUID.fromString("86000000-0000-0000-0000-000000000004");
    private static final UUID CATEGORY_ID = UUID.fromString("86000000-0000-0000-0000-000000000005");
    private static final Instant OCCURRED_AT = Instant.parse("2026-04-17T10:15:30Z");

    @Mock
    private TransactionInsightsService transactionInsightsService;

    @Mock
    private InsightsInbox insightsInbox;

    @InjectMocks
    private TransactionEventListener listener;

    @ParameterizedTest
    @MethodSource("amountCases")
    void onTransactionRecorded_whenEventArrives_shouldProjectSignedAmountAndState(BigDecimal amount, boolean expectedOutflow) {
        BaseEvent<TransactionRecorded> event = event(amount, true);

        listener.onTransactionRecorded(event);

        Runnable handler = captureHandler(event);
        handler.run();

        ArgumentCaptor<TransactionInsightRow> rowCaptor = ArgumentCaptor.forClass(TransactionInsightRow.class);
        verify(transactionInsightsService).save(rowCaptor.capture());
        TransactionInsightRow row = rowCaptor.getValue();
        assertThat(row.date()).isEqualTo(LocalDate.of(2026, 4, 17));
        assertThat(row.primaryCategoryId()).isEqualTo(CATEGORY_ID);
        assertThat(row.paymentChannel()).isEqualTo("in store");
        assertThat(row.amount()).isEqualByComparingTo(amount);
        assertThat(row.isOutflow()).isEqualTo(expectedOutflow);
        assertThat(row.isActive()).isTrue();
        assertThat(row.isoCurrencyCode()).isEqualTo("USD");
        assertThat(row.userId()).isEqualTo(USER_ID);
        assertThat(row.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(row.transactionId()).isEqualTo(TRANSACTION_ID);
        assertThat(row.updatedAt()).isEqualTo(OCCURRED_AT);
    }

    @Test
    void onTransactionRecorded_whenTransactionInactive_shouldProjectInactiveState() {
        BaseEvent<TransactionRecorded> event = event(new BigDecimal("-15.75"), false);

        listener.onTransactionRecorded(event);
        captureHandler(event).run();

        ArgumentCaptor<TransactionInsightRow> rowCaptor = ArgumentCaptor.forClass(TransactionInsightRow.class);
        verify(transactionInsightsService).save(rowCaptor.capture());
        assertThat(rowCaptor.getValue().isActive()).isFalse();
    }

    private Runnable captureHandler(BaseEvent<TransactionRecorded> event) {
        ArgumentCaptor<Runnable> handlerCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(insightsInbox).process(org.mockito.ArgumentMatchers.same(event), handlerCaptor.capture());
        return handlerCaptor.getValue();
    }

    private static Stream<Arguments> amountCases() {
        return Stream.of(
                Arguments.of(new BigDecimal("-15.75"), true),
                Arguments.of(new BigDecimal("0.00"), false),
                Arguments.of(new BigDecimal("100.00"), false));
    }

    private static BaseEvent<TransactionRecorded> event(BigDecimal amount, boolean active) {
        TransactionRecorded payload = new TransactionRecorded(
                TRANSACTION_ID,
                USER_ID,
                ACCOUNT_ID,
                amount,
                "USD",
                "Coffee",
                "place",
                LocalDate.of(2026, 4, 17),
                false,
                "in store",
                CATEGORY_ID,
                "FOOD_AND_DRINK_COFFEE",
                active);
        return new BaseEvent<>(
                EVENT_ID,
                "TransactionRecorded",
                "transaction-service",
                EVENT_ID,
                null,
                "transaction",
                TRANSACTION_ID,
                OCCURRED_AT,
                payload);
    }
}
