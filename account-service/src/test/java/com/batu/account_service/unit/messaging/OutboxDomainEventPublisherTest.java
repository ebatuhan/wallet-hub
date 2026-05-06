package com.batu.account_service.unit.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.batu.account_service.entity.OutboxEvent;
import com.batu.account_service.messaging.OutboxDomainEventPublisher;
import com.batu.account_service.repository.OutboxEventRepository;
import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.AccountRecorded;
import com.batu.shared.messaging.event.AccountRemoved;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OutboxDomainEventPublisherTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("a1000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("a1000000-0000-0000-0000-000000000002");
    private static final UUID CONNECTION_ID = UUID.fromString("a1000000-0000-0000-0000-000000000003");

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    void publishAccountRecorded_whenSerializationSucceeds_shouldPersistOutboxEvent() {
        OutboxDomainEventPublisher publisher = new OutboxDomainEventPublisher(outboxEventRepository, new JsonMapper());

        publisher.publishAccountRecorded(accountRecorded());

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent event = captor.getValue();
        assertThat(event.getRoutingKey()).isEqualTo(MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY);
        assertThat(event.getEventType()).isEqualTo(EventTypes.ACCOUNT_RECORDED);
        assertThat(event.getAggregateType()).isEqualTo("account");
        assertThat(event.getAggregateId()).isEqualTo(ACCOUNT_ID);
        assertThat(event.getPayload()).contains(EventTypes.ACCOUNT_RECORDED, ACCOUNT_ID.toString(), USER_ID.toString());
    }

    @Test
    void publishAccountRemoved_whenSerializationSucceeds_shouldPersistOutboxEvent() {
        OutboxDomainEventPublisher publisher = new OutboxDomainEventPublisher(outboxEventRepository, new JsonMapper());

        publisher.publishAccountRemoved(new AccountRemoved(ACCOUNT_ID, USER_ID, CONNECTION_ID));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getRoutingKey()).isEqualTo(MessagingTopology.ACCOUNT_REMOVED_ROUTING_KEY);
        assertThat(captor.getValue().getEventType()).isEqualTo(EventTypes.ACCOUNT_REMOVED);
        assertThat(captor.getValue().getPayload()).contains(EventTypes.ACCOUNT_REMOVED, ACCOUNT_ID.toString());
    }

    @Test
    void publishAccountRecorded_whenRepositorySaveFails_shouldThrowIllegalStateException() {
        OutboxDomainEventPublisher publisher = new OutboxDomainEventPublisher(outboxEventRepository, new JsonMapper());
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenThrow(new RuntimeException("database unavailable"));

        assertThatThrownBy(() -> publisher.publishAccountRecorded(accountRecorded()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to persist account outbox event");
    }

    @Test
    void publishAccountRemoved_whenRepositorySaveFails_shouldThrowIllegalStateException() {
        OutboxDomainEventPublisher publisher = new OutboxDomainEventPublisher(outboxEventRepository, new JsonMapper());
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenThrow(new RuntimeException("database unavailable"));

        assertThatThrownBy(() -> publisher.publishAccountRemoved(new AccountRemoved(ACCOUNT_ID, USER_ID, CONNECTION_ID)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to persist account outbox event");
    }

    private AccountRecorded accountRecorded() {
        return new AccountRecorded(
                ACCOUNT_ID,
                USER_ID,
                CONNECTION_ID,
                "Bank",
                "Checking",
                "depository",
                "checking",
                "1234",
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                "USD",
                true);
    }
}
