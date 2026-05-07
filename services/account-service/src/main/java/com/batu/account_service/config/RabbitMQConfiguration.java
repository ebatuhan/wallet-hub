package com.batu.account_service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import com.batu.shared.messaging.MessagingTopology;

@Configuration
public class RabbitMQConfiguration {

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    TopicExchange walletHubExchange() {
        return new TopicExchange(MessagingTopology.EXCHANGE_NAME, true, false);
    }
}
