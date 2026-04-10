package com.batu.transaction_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.batu.shared.messaging.saga.SagaChannels;

@Configuration
public class RabbitMQConfiguration {

    @Bean
    MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    TopicExchange sagaExchange() {
        return new TopicExchange(SagaChannels.EXCHANGE, true, false);
    }

    @Bean
    Declarables transactionSagaBindings(TopicExchange sagaExchange) {
        Queue persistInitialTransactionsCommandQueue = new Queue(
                SagaChannels.PERSIST_INITIAL_TRANSACTIONS_COMMAND_QUEUE,
                true);

        Binding persistInitialTransactionsCommandBinding = BindingBuilder.bind(persistInitialTransactionsCommandQueue)
                .to(sagaExchange)
                .with(SagaChannels.PERSIST_INITIAL_TRANSACTIONS_COMMAND_KEY);

        return new Declarables(
                sagaExchange,
                persistInitialTransactionsCommandQueue,
                persistInitialTransactionsCommandBinding);
    }
}
