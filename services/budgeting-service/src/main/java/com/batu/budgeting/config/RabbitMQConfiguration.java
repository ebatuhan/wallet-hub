package com.batu.budgeting.config;

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
    Queue budgetingTransactionPersistedQueue() {
        return new Queue(MessagingTopology.BUDGETING_TRANSACTION_PERSISTED_QUEUE, true);
    }

    @Bean
    Binding budgetingTransactionPersistedBinding(Queue budgetingTransactionPersistedQueue,
            TopicExchange analyticsExchange) {
        return BindingBuilder.bind(budgetingTransactionPersistedQueue)
                .to(analyticsExchange)
                .with(MessagingTopology.TRANSACTION_PERSISTED_ROUTING_KEY);
    }
}
