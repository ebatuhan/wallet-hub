package com.batu.insights_service.config;

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
    Queue transactionPersistedQueue() {
        return new Queue(MessagingTopology.TRANSACTION_PERSISTED_QUEUE, true);
    }

    @Bean
    Binding transactionPersistedBinding(Queue transactionPersistedQueue,
                                        TopicExchange walletHubExchange) {
        return BindingBuilder.bind(transactionPersistedQueue)
                .to(walletHubExchange)
                .with(MessagingTopology.TRANSACTION_PERSISTED_ROUTING_KEY);
    }

    @Bean
    Queue transactionRemovedQueue() {
        return new Queue(MessagingTopology.TRANSACTION_REMOVED_QUEUE, true);
    }

    @Bean
    Binding transactionRemovedBinding(Queue transactionRemovedQueue,
                                      TopicExchange walletHubExchange) {
        return BindingBuilder.bind(transactionRemovedQueue)
                .to(walletHubExchange)
                .with(MessagingTopology.TRANSACTION_REMOVED_ROUTING_KEY);
    }


    @Bean
    Queue accountPersistedQueue() {
        return new Queue(MessagingTopology.ACCOUNT_PERSISTED_QUEUE, true);
    }

    @Bean
    Binding accountPersistedBinding(Queue accountPersistedQueue,
                                    TopicExchange walletHubExchange) {
        return BindingBuilder.bind(accountPersistedQueue)
                .to(walletHubExchange)
                .with(MessagingTopology.ACCOUNT_PERSISTED_ROUTING_KEY);
    }

    @Bean
    Queue accountRemovedQueue() {
        return new Queue(MessagingTopology.ACCOUNT_REMOVED_QUEUE, true);
    }

    @Bean
    Binding accountRemovedBinding(Queue accountRemovedQueue,
                                  TopicExchange walletHubExchange) {
        return BindingBuilder.bind(accountRemovedQueue)
                .to(walletHubExchange)
                .with(MessagingTopology.ACCOUNT_REMOVED_ROUTING_KEY);
    }
}
