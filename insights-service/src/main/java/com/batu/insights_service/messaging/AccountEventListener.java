package com.batu.insights_service.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import io.micrometer.observation.annotation.Observed;

import com.batu.insights_service.entity.AccountBalanceDataPointRow;
import com.batu.insights_service.service.AccountInsightsService;
import com.batu.insights_service.service.TransactionInsightsService;
import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.AccountRecorded;
import com.batu.shared.messaging.event.AccountRemoved;

@Component
public class AccountEventListener {

    private final AccountInsightsService accountInsightsService;
    private final TransactionInsightsService transactionInsightsService;
    private final InsightsInbox insightsInbox;

    public AccountEventListener(AccountInsightsService accountInsightsService,
            TransactionInsightsService transactionInsightsService,
            InsightsInbox insightsInbox) {
        this.accountInsightsService = accountInsightsService;
        this.transactionInsightsService = transactionInsightsService;
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

    @RabbitListener(queues = MessagingTopology.ACCOUNT_REMOVED_QUEUE)
    @Observed(name = "insights.consume.account-removed", contextualName = "insights consume account removed")
    public void onAccountRemoved(BaseEvent<AccountRemoved> event) {
        insightsInbox.process(event, () -> {
            AccountRemoved message = event.getPayload();
            accountInsightsService.remove(message);
            transactionInsightsService.removeAccountTransactions(message.getAccountId(), message.getUserId());
        });
    }

}
