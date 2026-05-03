package com.batu.insights_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import io.micrometer.observation.annotation.Observed;

import com.batu.insights_service.entity.AccountBalanceDataPointRow;
import com.batu.insights_service.service.AccountInsightsService;
import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.AccountRecorded;

@Component
public class AccountEventListener {

    private final AccountInsightsService accountInsightsService;
    private final InsightsInbox insightsInbox;

    public AccountEventListener(AccountInsightsService accountInsightsService, InsightsInbox insightsInbox) {
        this.accountInsightsService = accountInsightsService;
        this.insightsInbox = insightsInbox;
    }

    @RabbitListener(queues = MessagingTopology.ACCOUNT_PERSISTED_QUEUE)
    @Observed(name = "insights.consume.account-recorded", contextualName = "insights consume account recorded")
    public void onAccountRecorded(BaseEvent<AccountRecorded> event) {
        insightsInbox.process(event, () -> {
            AccountRecorded message = event.getPayload();
            AccountBalanceDataPointRow accountBalanceDataPointRow = new AccountBalanceDataPointRow(
                    message.getAccountId(),
                    message.getUserId(),
                    message.getCurrentBalance(),
                    message.getIsoCurrencyCode(),
                    event.getOccurredAt().atZone(java.time.ZoneOffset.UTC).toLocalDate());

            accountInsightsService.save(accountBalanceDataPointRow);
        });
    }

}
