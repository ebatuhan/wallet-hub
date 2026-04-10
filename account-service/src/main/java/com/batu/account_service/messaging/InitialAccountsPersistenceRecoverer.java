package com.batu.account_service.messaging;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import com.batu.account_service.exception.AbstractApplicationException;
import com.batu.shared.messaging.saga.InitialAccountsPersistFailedEvent;
import com.batu.shared.messaging.saga.PersistInitialAccountsCommand;
import com.batu.shared.messaging.saga.SagaChannels;

@Component
public class InitialAccountsPersistenceRecoverer implements MessageRecoverer {

    private static final String UNEXPECTED_ERROR = "UNEXPECTED_ERROR";

    private final MessageConverter messageConverter;
    private final RabbitTemplate rabbitTemplate;

    public InitialAccountsPersistenceRecoverer(MessageConverter messageConverter, RabbitTemplate rabbitTemplate) {
        this.messageConverter = messageConverter;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void recover(Message message, Throwable cause) {
        Object payload = messageConverter.fromMessage(message);

        if (payload instanceof PersistInitialAccountsCommand command) {
            Throwable failure = unwrap(cause);

            rabbitTemplate.convertAndSend(
                    SagaChannels.EXCHANGE,
                    SagaChannels.INITIAL_ACCOUNTS_PERSIST_FAILED_EVENT_KEY,
                    new InitialAccountsPersistFailedEvent(
                            command.getSagaId(),
                            command.getConnectionId(),
                            errorCodeOf(failure),
                            errorMessageOf(failure)));
        }
    }

    private String errorCodeOf(Throwable throwable) {
        if (throwable instanceof AbstractApplicationException ex) {
            return ex.getCode();
        }

        return UNEXPECTED_ERROR;
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
