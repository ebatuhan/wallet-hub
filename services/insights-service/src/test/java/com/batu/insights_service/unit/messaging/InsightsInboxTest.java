package com.batu.insights_service.unit.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.batu.insights_service.entity.InboxEvent;
import com.batu.insights_service.messaging.InsightsInbox;
import com.batu.insights_service.repository.InboxEventRepository;
import com.batu.shared.messaging.BaseEvent;

@ExtendWith(MockitoExtension.class)
class InsightsInboxTest {

    private static final UUID EVENT_ID = UUID.fromString("88000000-0000-0000-0000-000000000001");

    @Mock
    private InboxEventRepository inboxEventRepository;

    @InjectMocks
    private InsightsInbox insightsInbox;

    @Test
    void process_whenEventAlreadyProcessed_shouldNotRunHandlerOrInsertInboxRow() {
        BaseEvent<String> event = event();
        AtomicBoolean handled = new AtomicBoolean(false);
        when(inboxEventRepository.existsById(EVENT_ID)).thenReturn(true);

        insightsInbox.process(event, () -> handled.set(true));

        assertThat(handled).isFalse();
        verify(inboxEventRepository).lockByEventId(EVENT_ID);
        verify(inboxEventRepository, never()).save(org.mockito.ArgumentMatchers.any(InboxEvent.class));
    }

    @Test
    void process_whenEventIsNew_shouldRunHandlerThenInsertInboxRow() {
        BaseEvent<String> event = event();
        AtomicBoolean handled = new AtomicBoolean(false);
        when(inboxEventRepository.existsById(EVENT_ID)).thenReturn(false);

        insightsInbox.process(event, () -> handled.set(true));

        assertThat(handled).isTrue();
        verify(inboxEventRepository).lockByEventId(EVENT_ID);
        verify(inboxEventRepository).save(org.mockito.ArgumentMatchers.argThat(eventRow -> eventRow.getEventId().equals(EVENT_ID)));
    }

    @Test
    void process_whenHandlerFails_shouldPropagateAndNotInsertInboxRowSoRedeliveryCanRetry() {
        BaseEvent<String> event = event();
        RuntimeException failure = new RuntimeException("projection failed");
        when(inboxEventRepository.existsById(EVENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> insightsInbox.process(event, () -> {
            throw failure;
        })).isSameAs(failure);

        verify(inboxEventRepository).lockByEventId(EVENT_ID);
        verify(inboxEventRepository, never()).save(org.mockito.ArgumentMatchers.any(InboxEvent.class));
    }

    private static BaseEvent<String> event() {
        return new BaseEvent<>(
                EVENT_ID,
                "TestEvent",
                "test",
                EVENT_ID,
                null,
                "test",
                UUID.fromString("88000000-0000-0000-0000-000000000002"),
                Instant.parse("2026-04-17T10:15:30Z"),
                "payload");
    }
}
