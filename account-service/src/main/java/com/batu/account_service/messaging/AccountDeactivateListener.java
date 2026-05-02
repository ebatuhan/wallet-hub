package com.batu.account_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.ConnectionRemoved;

import io.micrometer.observation.annotation.Observed;

@Component
public class AccountDeactivateListener {

    private final AccountEventHandler accountEventHandler;

    public AccountDeactivateListener(AccountEventHandler accountEventHandler) {
        this.accountEventHandler = accountEventHandler;
    }

    @RabbitListener(queues = MessagingTopology.ACCOUNT_DEACTIVATE_QUEUE)
    @Observed(name = "accounts.deactivate.consume", contextualName = "accounts consume deactivate")
    public void consume(BaseEvent<ConnectionRemoved> event) {
        accountEventHandler.handleConnectionRemoved(event);
    }
}
