package com.batu.budgeting.unit.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.batu.budgeting.entity.InboxEvent;
import com.batu.budgeting.messaging.BudgetingInbox;
import com.batu.budgeting.repository.InboxEventRepository;
import com.batu.shared.messaging.BaseEvent;

@ExtendWith(MockitoExtension.class)
class BudgetingInboxTest {

    private static final UUID EVENT_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");

    @Mock
    private InboxEventRepository inboxEventRepository;

    @Test
    void process_whenEventAlreadyExists_shouldLockEventThenSkipHandlerAndNotSaveInboxEvent() {
        BudgetingInbox budgetingInbox = new BudgetingInbox(inboxEventRepository);
        Runnable handler = org.mockito.Mockito.mock(Runnable.class);
        when(inboxEventRepository.existsById(EVENT_ID)).thenReturn(true);

        budgetingInbox.process(event(), handler);

        verify(inboxEventRepository).lockByEventId(EVENT_ID);
        verify(handler, never()).run();
        verify(inboxEventRepository, never()).save(any());
    }

    @Test
    void process_whenEventIsNew_shouldLockEventRunHandlerAndSaveInboxEvent() {
        BudgetingInbox budgetingInbox = new BudgetingInbox(inboxEventRepository);
        Runnable handler = org.mockito.Mockito.mock(Runnable.class);
        ArgumentCaptor<InboxEvent> inboxEventCaptor = ArgumentCaptor.forClass(InboxEvent.class);
        when(inboxEventRepository.existsById(EVENT_ID)).thenReturn(false);

        budgetingInbox.process(event(), handler);

        InOrder inOrder = inOrder(inboxEventRepository, handler);
        inOrder.verify(inboxEventRepository).lockByEventId(EVENT_ID);
        inOrder.verify(handler).run();
        inOrder.verify(inboxEventRepository).save(inboxEventCaptor.capture());
        verify(handler).run();
        org.assertj.core.api.Assertions.assertThat(inboxEventCaptor.getValue().getEventId()).isEqualTo(EVENT_ID);
    }

    @Test
    void process_whenHandlerFails_shouldPropagateExceptionAndNotSaveInboxEvent() {
        BudgetingInbox budgetingInbox = new BudgetingInbox(inboxEventRepository);
        RuntimeException failure = new RuntimeException("boom");
        when(inboxEventRepository.existsById(EVENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> budgetingInbox.process(event(), () -> {
            throw failure;
        })).isSameAs(failure);

        verify(inboxEventRepository).lockByEventId(EVENT_ID);
        verify(inboxEventRepository, never()).save(any());
    }

    private BaseEvent<String> event() {
        return new BaseEvent<>(
                EVENT_ID,
                "TransactionRecorded",
                "transaction-service",
                EVENT_ID,
                null,
                "transaction",
                UUID.fromString("30000000-0000-0000-0000-000000000002"),
                Instant.parse("2026-05-06T10:15:30Z"),
                "payload");
    }
}
