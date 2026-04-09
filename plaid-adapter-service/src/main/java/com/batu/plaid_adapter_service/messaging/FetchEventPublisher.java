package com.batu.plaid_adapter_service.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.batu.shared.messaging.saga.SagaChannels;
import com.batu.shared.messaging.saga.StartLinkSagaCommand;

@Component
public class FetchEventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public FetchEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void RequestLinkSaga(StartLinkSagaCommand startLinkSagaCommand) {
        rabbitTemplate.convertAndSend(SagaChannels.EXCHANGE,
                SagaChannels.START_LINK_SAGA_KEY,
                startLinkSagaCommand);
    }

}
