package com.batu.account_service.unit.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    @ParameterizedTest
    @MethodSource("eventPublishCases")
    void publish_whenSerializationSucceeds_shouldPersistOutboxEvent(
            String caseName,
            Consumer<OutboxDomainEventPublisher> publishAction,
            String expectedRoutingKey,
            String expectedEventType) {
        OutboxDomainEventPublisher publisher = new OutboxDomainEventPublisher(outboxEventRepository, new JsonMapper());

        publishAction.accept(publisher);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        OutboxEvent event = captor.getValue();
        assertThat(event.getRoutingKey()).isEqualTo(expectedRoutingKey);
        assertThat(event.getEventType()).isEqualTo(expectedEventType);
        assertThat(event.getAggregateType()).isEqualTo("account");
        assertThat(event.getAggregateId()).isEqualTo(ACCOUNT_ID);
        assertThat(event.getPayload()).contains(expectedEventType, ACCOUNT_ID.toString(), USER_ID.toString());
    }

    @ParameterizedTest
    @MethodSource("eventPublishCases")
    void publish_whenRepositorySaveFails_shouldThrowIllegalStateException(
            String caseName,
            Consumer<OutboxDomainEventPublisher> publishAction,
            String expectedRoutingKey,
            String expectedEventType) {
        OutboxDomainEventPublisher publisher = new OutboxDomainEventPublisher(outboxEventRepository, new JsonMapper());
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenThrow(new RuntimeException("database unavailable"));

        assertThatThrownBy(() -> publishAction.accept(publisher))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unable to persist account outbox event");
    }

    private static Stream<Arguments> eventPublishCases() {
        return Stream.of(
                Arguments.of(
                        "recorded",
                        (Consumer<OutboxDomainEventPublisher>) publisher -> publisher.publishAccountRecorded(accountRecorded()),
                        MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY,
                        EventTypes.ACCOUNT_RECORDED),
                Arguments.of(
                        "removed",
                        (Consumer<OutboxDomainEventPublisher>) publisher -> publisher
                                .publishAccountRemoved(new AccountRemoved(ACCOUNT_ID, USER_ID, CONNECTION_ID)),
                        MessagingTopology.ACCOUNT_REMOVED_ROUTING_KEY,
                        EventTypes.ACCOUNT_REMOVED));
    }

    private static AccountRecorded accountRecorded() {
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
