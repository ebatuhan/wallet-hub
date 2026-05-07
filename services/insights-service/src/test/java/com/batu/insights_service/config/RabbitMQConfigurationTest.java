package com.batu.insights_service.config;

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
    void walletHubExchange_whenCreated_shouldUseDurableTopicExchange() {
        TopicExchange exchange = configuration.walletHubExchange();

        assertThat(exchange.getName()).isEqualTo(MessagingTopology.EXCHANGE_NAME);
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    void transactionPersistedBinding_whenCreated_shouldBindRecordedQueueToRecordedRoutingKey() {
        Queue queue = configuration.transactionPersistedQueue();
        TopicExchange exchange = configuration.walletHubExchange();

        Binding binding = configuration.transactionPersistedBinding(queue, exchange);

        assertQueue(queue, MessagingTopology.TRANSACTION_PERSISTED_QUEUE);
        assertBinding(binding, MessagingTopology.TRANSACTION_PERSISTED_QUEUE, MessagingTopology.TRANSACTION_PERSISTED_ROUTING_KEY);
    }

    @Test
    void accountPersistedBinding_whenCreated_shouldBindRecordedQueueToRecordedRoutingKey() {
        Queue queue = configuration.accountPersistedQueue();
        TopicExchange exchange = configuration.walletHubExchange();

        Binding binding = configuration.accountPersistedBinding(queue, exchange);

        assertQueue(queue, MessagingTopology.ACCOUNT_PERSISTED_QUEUE);
        assertBinding(binding, MessagingTopology.ACCOUNT_PERSISTED_QUEUE, MessagingTopology.ACCOUNT_PERSISTED_ROUTING_KEY);
    }

    @Test
    void accountRemovedBinding_whenCreated_shouldBindRemovedQueueToRemovedRoutingKey() {
        Queue queue = configuration.accountRemovedQueue();
        TopicExchange exchange = configuration.walletHubExchange();

        Binding binding = configuration.accountRemovedBinding(queue, exchange);

        assertQueue(queue, MessagingTopology.ACCOUNT_REMOVED_QUEUE);
        assertBinding(binding, MessagingTopology.ACCOUNT_REMOVED_QUEUE, MessagingTopology.ACCOUNT_REMOVED_ROUTING_KEY);
    }

    private static void assertQueue(Queue queue, String expectedName) {
        assertThat(queue.getName()).isEqualTo(expectedName);
        assertThat(queue.isDurable()).isTrue();
    }

    private static void assertBinding(Binding binding, String expectedQueue, String expectedRoutingKey) {
        assertThat(binding.getDestination()).isEqualTo(expectedQueue);
        assertThat(binding.getExchange()).isEqualTo(MessagingTopology.EXCHANGE_NAME);
        assertThat(binding.getRoutingKey()).isEqualTo(expectedRoutingKey);
        assertThat(binding.isDestinationQueue()).isTrue();
    }
}
