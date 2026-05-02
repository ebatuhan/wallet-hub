package com.batu.account_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
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

    @Bean
    Queue accountSyncQueue() {
        return new Queue(MessagingTopology.ACCOUNT_SYNC_QUEUE, true);
    }

    @Bean
    Queue accountDeactivateQueue() {
        return new Queue(MessagingTopology.ACCOUNT_DEACTIVATE_QUEUE, true);
    }

    @Bean
    Binding accountSyncBinding(Queue accountSyncQueue, TopicExchange walletHubExchange) {
        return BindingBuilder.bind(accountSyncQueue)
                .to(walletHubExchange)
                .with(MessagingTopology.ACCOUNT_SYNC_ROUTING_KEY);
    }

    @Bean
    Binding accountDeactivateBinding(Queue accountDeactivateQueue, TopicExchange walletHubExchange) {
        return BindingBuilder.bind(accountDeactivateQueue)
                .to(walletHubExchange)
                .with(MessagingTopology.ACCOUNT_DEACTIVATE_ROUTING_KEY);
    }
}
