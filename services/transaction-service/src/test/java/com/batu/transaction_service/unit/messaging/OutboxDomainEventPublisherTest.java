package com.batu.transaction_service.unit.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.TransactionRecorded;
import com.batu.shared.messaging.event.TransactionRemoved;
import com.batu.transaction_service.entity.OutboxEvent;
import com.batu.transaction_service.messaging.OutboxDomainEventPublisher;
import com.batu.transaction_service.repository.OutboxEventRepository;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OutboxDomainEventPublisherTest {

    private static final UUID TRANSACTION_ID = UUID.fromString("e4000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("e4000000-0000-0000-0000-000000000002");
    private static final UUID ACCOUNT_ID = UUID.fromString("e4000000-0000-0000-0000-000000000003");
    private static final UUID PRIMARY_CATEGORY_ID = UUID.fromString("e4000000-0000-0000-0000-000000000004");

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
        assertThat(event.getAggregateType()).isEqualTo("transaction");
        assertThat(event.getAggregateId()).isEqualTo(TRANSACTION_ID);
        assertThat(event.getPayload()).contains(expectedEventType, TRANSACTION_ID.toString(), USER_ID.toString());
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
                .hasMessage("Unable to persist transaction outbox event");
    }

    private static Stream<Arguments> eventPublishCases() {
        return Stream.of(
                Arguments.of(
                        "recorded",
                        (Consumer<OutboxDomainEventPublisher>) publisher -> publisher.publishTransactionRecorded(transactionRecorded()),
                        MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY,
                        EventTypes.TRANSACTION_RECORDED),
                Arguments.of(
                        "removed",
                        (Consumer<OutboxDomainEventPublisher>) publisher -> publisher
                                .publishTransactionRemoved(new TransactionRemoved(TRANSACTION_ID, USER_ID, ACCOUNT_ID)),
                        MessagingTopology.TRANSACTION_REMOVED_ROUTING_KEY,
                        EventTypes.TRANSACTION_REMOVED));
    }

    private static TransactionRecorded transactionRecorded() {
        return new TransactionRecorded(
                TRANSACTION_ID,
                USER_ID,
                ACCOUNT_ID,
                new BigDecimal("42.50"),
                "USD",
                "Coffee Shop",
                "place",
                LocalDate.of(2026, 5, 7),
                false,
                "in store",
                PRIMARY_CATEGORY_ID,
                "FOOD_AND_DRINK",
                true);
    }
}
