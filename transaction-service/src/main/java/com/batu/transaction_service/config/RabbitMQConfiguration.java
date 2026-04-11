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
    TopicExchange analyticsExchange() {
        return new TopicExchange(MessagingTopology.EXCHANGE_NAME, true, false);
    }

    @Bean
    Queue transactionPersistedQueue() {
        return new Queue(MessagingTopology.TRANSACTION_PERSISTED_QUEUE, true);
    }

    @Bean
    Binding transactionPersistedBinding(Queue transactionPersistedQueue, TopicExchange analyticsExchange) {
        return BindingBuilder.bind(transactionPersistedQueue)
                .to(analyticsExchange)
                .with(MessagingTopology.TRANSACTION_PERSISTED_ROUTING_KEY);
    }

}
