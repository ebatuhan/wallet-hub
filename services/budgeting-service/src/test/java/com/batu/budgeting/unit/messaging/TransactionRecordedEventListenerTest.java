package com.batu.budgeting.unit.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.batu.budgeting.messaging.BudgetingInbox;
import com.batu.budgeting.messaging.TransactionRecordedEventListener;
import com.batu.budgeting.service.BudgetService;
import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.event.TransactionRecorded;

@ExtendWith(MockitoExtension.class)
class TransactionRecordedEventListenerTest {

    @Mock
    private BudgetService budgetService;

    @Mock
    private BudgetingInbox budgetingInbox;

    @Test
    void onTransactionRecorded_whenEventArrives_shouldDelegateProcessingThroughInbox() {
        TransactionRecordedEventListener listener = new TransactionRecordedEventListener(budgetService, budgetingInbox);
        BaseEvent<TransactionRecorded> event = event();
        ArgumentCaptor<Runnable> handlerCaptor = ArgumentCaptor.forClass(Runnable.class);

        listener.onTransactionRecorded(event);

        verify(budgetingInbox).process(org.mockito.Mockito.eq(event), handlerCaptor.capture());

        handlerCaptor.getValue().run();

        verify(budgetService).applyTransaction(event.getPayload());
    }

    private BaseEvent<TransactionRecorded> event() {
        UUID transactionId = UUID.fromString("40000000-0000-0000-0000-000000000001");
        TransactionRecorded payload = new TransactionRecorded(
                transactionId,
                UUID.fromString("40000000-0000-0000-0000-000000000002"),
                UUID.fromString("40000000-0000-0000-0000-000000000003"),
                new BigDecimal("-25.00"),
                "USD",
                "Coffee",
                "PLACE",
                LocalDate.of(2026, 5, 6),
                false,
                "in store",
                UUID.fromString("40000000-0000-0000-0000-000000000004"),
                "FOOD_AND_DRINK",
                true);
        return new BaseEvent<>(
                UUID.fromString("40000000-0000-0000-0000-000000000005"),
                EventTypes.TRANSACTION_RECORDED,
                "transaction-service",
                UUID.fromString("40000000-0000-0000-0000-000000000005"),
                null,
                "transaction",
                transactionId,
                Instant.parse("2026-05-06T10:15:30Z"),
                payload);
    }
}
