package com.batu.account_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.batu.account_service.service.AccountService;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.command.AccountSyncCommand;

@Component
public class AccountSyncCommandListener {

    private final AccountService accountService;

    public AccountSyncCommandListener(AccountService accountService) {
        this.accountService = accountService;
    }

    @RabbitListener(queues = MessagingTopology.ACCOUNT_CREATE_QUEUE)
    public void onAccountCreate(AccountSyncCommand command) {
        accountService.create(command);
    }

    @RabbitListener(queues = MessagingTopology.ACCOUNT_UPDATE_QUEUE)
    public void onAccountUpdate(AccountSyncCommand command) {
        accountService.update(command);
    }
}
