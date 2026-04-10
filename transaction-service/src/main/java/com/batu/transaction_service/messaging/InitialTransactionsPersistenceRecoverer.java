package com.batu.transaction_service.messaging;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import com.batu.shared.messaging.saga.InitialTransactionsPersistFailedEvent;
import com.batu.shared.messaging.saga.PersistInitialTransactionsCommand;
import com.batu.shared.messaging.saga.SagaChannels;

@Component
public class InitialTransactionsPersistenceRecoverer implements MessageRecoverer {

    private static final String UNEXPECTED_ERROR = "UNEXPECTED_ERROR";

    private final MessageConverter messageConverter;
    private final RabbitTemplate rabbitTemplate;

    public InitialTransactionsPersistenceRecoverer(MessageConverter messageConverter, RabbitTemplate rabbitTemplate) {
        this.messageConverter = messageConverter;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        Object payload = messageConverter.fromMessage(message);

        if (payload instanceof PersistInitialTransactionsCommand command) {
            Throwable failure = unwrap(cause);

            rabbitTemplate.convertAndSend(
                    SagaChannels.EXCHANGE,
                    SagaChannels.INITIAL_TRANSACTIONS_PERSIST_FAILED_EVENT_KEY,
                    new InitialTransactionsPersistFailedEvent(
                            command.getSagaId(),
                            command.getConnectionId(),
                            UNEXPECTED_ERROR,
                            errorMessageOf(failure)));
        }
    }

    private String errorMessageOf(Throwable throwable) {
        String message = throwable.getMessage();
        return (message == null || message.isBlank()) ? "Unexpected message handling failure." : message;
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null) {
            current = current.getCause();
        }

        return current;
    }
}
