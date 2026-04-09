package com.batu.saga_orchestration.config;

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
    Declarables sagaBindings(TopicExchange sagaExchange) {
        Queue startLinkSagaQueue = new Queue(SagaChannels.START_LINK_SAGA_QUEUE, true);

        Queue fetchAccountsCommandQueue = new Queue(SagaChannels.FETCH_ACCOUNTS_COMMAND_QUEUE, true);
        Queue fetchAccountsEventQueue = new Queue(SagaChannels.FETCH_ACCOUNTS_EVENT_QUEUE, true);
        Queue fetchAccountsCompensateQueue = new Queue(SagaChannels.FETCH_ACCOUNTS_COMPENSATE_QUEUE, true);

        Queue persistAccountsCommandQueue = new Queue(SagaChannels.PERSIST_ACCOUNTS_COMMAND_QUEUE, true);
        Queue persistAccountsEventQueue = new Queue(SagaChannels.PERSIST_ACCOUNTS_EVENT_QUEUE, true);

        Queue fetchTransactionsCommandQueue = new Queue(SagaChannels.FETCH_TRANSACTIONS_COMMAND_QUEUE, true);
        Queue fetchTransactionsEventQueue = new Queue(SagaChannels.FETCH_TRANSACTIONS_EVENT_QUEUE, true);
        Queue fetchTransactionsCompensateQueue = new Queue(SagaChannels.FETCH_TRANSACTIONS_COMPENSATE_QUEUE, true);

        Binding startLinkSagaBinding = BindingBuilder.bind(startLinkSagaQueue)
                .to(sagaExchange)
                .with(SagaChannels.START_LINK_SAGA_KEY);

        Binding fetchAccountsCommandBinding = BindingBuilder.bind(fetchAccountsCommandQueue)
                .to(sagaExchange)
                .with(SagaChannels.FETCH_ACCOUNTS_COMMAND_KEY);

        Binding fetchAccountsEventBinding = BindingBuilder.bind(fetchAccountsEventQueue)
                .to(sagaExchange)
                .with(SagaChannels.FETCH_ACCOUNTS_EVENT_KEY);

        Binding fetchAccountsCompensateBinding = BindingBuilder.bind(fetchAccountsCompensateQueue)
                .to(sagaExchange)
                .with(SagaChannels.FETCH_ACCOUNTS_COMPENSATE_KEY);

        Binding persistAccountsCommandBinding = BindingBuilder.bind(persistAccountsCommandQueue)
                .to(sagaExchange)
                .with(SagaChannels.PERSIST_ACCOUNTS_COMMAND_KEY);

        Binding persistAccountsEventBinding = BindingBuilder.bind(persistAccountsEventQueue)
                .to(sagaExchange)
                .with(SagaChannels.PERSIST_ACCOUNTS_EVENT_KEY);

        Binding fetchTransactionsCommandBinding = BindingBuilder.bind(fetchTransactionsCommandQueue)
                .to(sagaExchange)
                .with(SagaChannels.FETCH_TRANSACTIONS_COMMAND_KEY);

        Binding fetchTransactionsEventBinding = BindingBuilder.bind(fetchTransactionsEventQueue)
                .to(sagaExchange)
                .with(SagaChannels.FETCH_TRANSACTIONS_EVENT_KEY);

        Binding fetchTransactionsCompensateBinding = BindingBuilder.bind(fetchTransactionsCompensateQueue)
                .to(sagaExchange)
                .with(SagaChannels.FETCH_TRANSACTIONS_COMPENSATE_KEY);

        return new Declarables(
                sagaExchange,
                startLinkSagaQueue,

                fetchAccountsCommandQueue,
                fetchAccountsEventQueue,
                fetchAccountsCompensateQueue,

                persistAccountsCommandQueue,
                persistAccountsEventQueue,

                fetchTransactionsCommandQueue,
                fetchTransactionsEventQueue,
                fetchTransactionsCompensateQueue,

                startLinkSagaBinding,

                fetchAccountsCommandBinding,
                fetchAccountsEventBinding,
                fetchAccountsCompensateBinding,

                persistAccountsCommandBinding,
                persistAccountsEventBinding,

                fetchTransactionsCommandBinding,
                fetchTransactionsEventBinding,
                fetchTransactionsCompensateBinding
        );
    }
}