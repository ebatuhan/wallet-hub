package com.batu.plaid_adapter_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;

import com.batu.shared.messaging.outbox.EventMessagePublisher;
import com.batu.shared.messaging.outbox.OutboxEventStore;
import com.batu.shared.messaging.outbox.OutboxRelay;
import com.batu.shared.messaging.outbox.OutboxService;

import tools.jackson.databind.ObjectMapper;

@Configuration
public class MessagingInfrastructureConfig {
    @Bean("outboxObjectMapper")
    ObjectMapper outboxObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    OutboxService outboxService(OutboxEventStore outboxEventStore,
            @Qualifier("outboxObjectMapper") ObjectMapper objectMapper) {
        return new OutboxService(outboxEventStore, objectMapper);
    }

    @Bean
    OutboxRelay outboxRelay(OutboxEventStore outboxEventStore, EventMessagePublisher eventMessagePublisher) {
        return new OutboxRelay(outboxEventStore, eventMessagePublisher);
    }
}
