package com.batu.account_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.AccountObserved;

import io.micrometer.observation.annotation.Observed;

@Component
public class AccountSyncListener {

    private final AccountEventHandler accountEventHandler;

    public AccountSyncListener(AccountEventHandler accountEventHandler) {
        this.accountEventHandler = accountEventHandler;
    }

    @RabbitListener(queues = MessagingTopology.ACCOUNT_SYNC_QUEUE)
    @Observed(name = "accounts.sync.consume", contextualName = "accounts consume sync")
    public void consume(BaseEvent<AccountObserved> event) {
        accountEventHandler.handleAccountObserved(event);
    }
}
