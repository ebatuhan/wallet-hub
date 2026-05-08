package com.batu.transaction_service.config;

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
    void accountRemovedQueueAndBinding_whenCreated_shouldUseTransactionQueueAndAccountRemovedRoutingKey() {
        TopicExchange exchange = configuration.walletHubExchange();
        Queue queue = configuration.accountRemovedQueue();
        Binding binding = configuration.accountRemovedBinding(queue, exchange);

        assertThat(queue.getName()).isEqualTo(MessagingTopology.TRANSACTION_ACCOUNT_REMOVED_QUEUE);
        assertThat(queue.isDurable()).isTrue();
        assertThat(binding.getDestination()).isEqualTo(MessagingTopology.TRANSACTION_ACCOUNT_REMOVED_QUEUE);
        assertThat(binding.getExchange()).isEqualTo(MessagingTopology.EXCHANGE_NAME);
        assertThat(binding.getRoutingKey()).isEqualTo(MessagingTopology.ACCOUNT_REMOVED_ROUTING_KEY);
        assertThat(binding.isDestinationQueue()).isTrue();
    }
}
