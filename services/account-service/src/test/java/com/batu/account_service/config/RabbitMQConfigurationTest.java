package com.batu.account_service.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
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
}
