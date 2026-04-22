package com.batu.insights_service.messaging;

import java.time.LocalDate;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.batu.insights_service.entity.AccountBalanceDataPointRow;
import com.batu.insights_service.service.AccountInsightsService;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.AccountPersistedEvent;

@Component
public class AccountEventListener {

    private final AccountInsightsService accountInsightsService;

    public AccountEventListener(AccountInsightsService accountInsightsService) {
        this.accountInsightsService = accountInsightsService;
    }

    @RabbitListener(queues = MessagingTopology.ACCOUNT_PERSISTED_QUEUE)
    public void onAccountPersistedEvent(AccountPersistedEvent message) {
        AccountBalanceDataPointRow accountBalanceDataPointRow = new AccountBalanceDataPointRow(message.getAccountId(),
                message.getUserId(),
                message.getCurrentBalance(),
                message.getIsoCurrencyCode(),
                message.getOccurredAt().atZone(java.time.ZoneOffset.UTC).toLocalDate());

        accountInsightsService.save(accountBalanceDataPointRow);
    }

}
