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
    TopicExchange analyticsExchange() {
        return new TopicExchange(MessagingTopology.EXCHANGE_NAME, true, false);
    }

    @Bean
    TopicExchange syncCommandExchange() {
        return new TopicExchange(MessagingTopology.COMMAND_EXCHANGE_NAME, true, false);
    }

    @Bean
    Queue accountPersistedQueue() {
        return new Queue(MessagingTopology.ACCOUNT_PERSISTED_QUEUE, true);
    }

    @Bean
    Binding accountPersistedBinding(Queue accountPersistedQueue, TopicExchange analyticsExchange) {
        return BindingBuilder.bind(accountPersistedQueue)
                .to(analyticsExchange)
                .with(MessagingTopology.ACCOUNT_PERSISTED_ROUTING_KEY);
    }

    @Bean
    Queue accountCreateQueue() {
        return new Queue(MessagingTopology.ACCOUNT_CREATE_QUEUE, true);
    }

    @Bean
    Binding accountCreateBinding(Queue accountCreateQueue, TopicExchange syncCommandExchange) {
        return BindingBuilder.bind(accountCreateQueue)
                .to(syncCommandExchange)
                .with(MessagingTopology.ACCOUNT_CREATE_ROUTING_KEY);
    }

    @Bean
    Queue accountUpdateQueue() {
        return new Queue(MessagingTopology.ACCOUNT_UPDATE_QUEUE, true);
    }

    @Bean
    Binding accountUpdateBinding(Queue accountUpdateQueue, TopicExchange syncCommandExchange) {
        return BindingBuilder.bind(accountUpdateQueue)
                .to(syncCommandExchange)
                .with(MessagingTopology.ACCOUNT_UPDATE_ROUTING_KEY);
    }
}
