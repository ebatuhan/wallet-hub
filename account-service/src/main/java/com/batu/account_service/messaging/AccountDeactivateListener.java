package com.batu.account_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.batu.account_service.service.AccountService;
import com.batu.shared.dto.request.AccountDeactivateRequestDto;
import com.batu.shared.messaging.MessagingTopology;

import io.micrometer.observation.annotation.Observed;

@Component
public class AccountDeactivateListener {

    private final AccountService accountService;

    public AccountDeactivateListener(AccountService accountService) {
        this.accountService = accountService;
    }

    @RabbitListener(queues = MessagingTopology.ACCOUNT_DEACTIVATE_QUEUE)
    @Observed(name = "accounts.deactivate.consume", contextualName = "accounts consume deactivate")
    public void consume(AccountDeactivateRequestDto request) {
        accountService.deactivateFromSync(request.getAccountId(), request.getSyncVersion());
    }
}
