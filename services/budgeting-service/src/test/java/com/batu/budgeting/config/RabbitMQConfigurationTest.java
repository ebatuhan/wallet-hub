package com.batu.budgeting.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

import com.batu.shared.messaging.MessagingTopology;

class RabbitMQConfigurationTest {

    private final RabbitMQConfiguration configuration = new RabbitMQConfiguration();

    @Test
    void messageConverter_whenCreated_shouldUseJacksonJsonConverter() {
        assertThat(configuration.messageConverter()).isInstanceOf(JacksonJsonMessageConverter.class);
    }

    @Test
    void analyticsExchange_whenCreated_shouldUseDurableTopicExchange() {
        TopicExchange exchange = configuration.analyticsExchange();

        assertThat(exchange.getName()).isEqualTo(MessagingTopology.EXCHANGE_NAME);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    void budgetingTransactionPersistedBinding_whenCreated_shouldBindQueueToTransactionPersistedRoutingKey() {
        Queue queue = configuration.budgetingTransactionPersistedQueue();
        TopicExchange exchange = configuration.analyticsExchange();

        Binding binding = configuration.budgetingTransactionPersistedBinding(queue, exchange);

        assertThat(queue.getName()).isEqualTo(MessagingTopology.BUDGETING_TRANSACTION_PERSISTED_QUEUE);
        assertThat(queue.isDurable()).isTrue();
        assertThat(binding.getDestination()).isEqualTo(MessagingTopology.BUDGETING_TRANSACTION_PERSISTED_QUEUE);
        assertThat(binding.getExchange()).isEqualTo(MessagingTopology.EXCHANGE_NAME);
        assertThat(binding.getRoutingKey()).isEqualTo(MessagingTopology.TRANSACTION_PERSISTED_ROUTING_KEY);
        assertThat(binding.isDestinationQueue()).isTrue();
    }
}
