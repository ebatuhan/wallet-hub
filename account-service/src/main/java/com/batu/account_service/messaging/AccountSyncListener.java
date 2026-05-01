package com.batu.account_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.batu.account_service.service.AccountService;
import com.batu.shared.dto.request.AccountRequestDto;
import com.batu.shared.messaging.MessagingTopology;

import io.micrometer.observation.annotation.Observed;

@Component
public class AccountSyncListener {

    private final AccountService accountService;

    public AccountSyncListener(AccountService accountService) {
        this.accountService = accountService;
    }

    @RabbitListener(queues = MessagingTopology.ACCOUNT_SYNC_QUEUE)
    @Observed(name = "accounts.sync.consume", contextualName = "accounts consume sync")
    public void consume(AccountRequestDto request) {
        accountService.upsertFromSync(request);
    }
}
