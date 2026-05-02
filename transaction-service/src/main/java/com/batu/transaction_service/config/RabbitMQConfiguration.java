package com.batu.transaction_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    Queue transactionSyncQueue() {
        return new Queue(MessagingTopology.TRANSACTION_SYNC_QUEUE, true);
    }

    @Bean
    Queue accountRemovedQueue() {
        return new Queue(MessagingTopology.ACCOUNT_REMOVED_TRANSACTION_QUEUE, true);
    }

    @Bean
    Binding transactionSyncBinding(Queue transactionSyncQueue, TopicExchange walletHubExchange) {
        return BindingBuilder.bind(transactionSyncQueue)
                .to(walletHubExchange)
                .with(MessagingTopology.TRANSACTION_SYNC_ROUTING_KEY);
    }

    @Bean
    Binding accountRemovedBinding(Queue accountRemovedQueue, TopicExchange walletHubExchange) {
        return BindingBuilder.bind(accountRemovedQueue)
                .to(walletHubExchange)
                .with(MessagingTopology.ACCOUNT_REMOVED_ROUTING_KEY);
    }
}
