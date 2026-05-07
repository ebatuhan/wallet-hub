package com.batu.shared.unit.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;

class MessagingTopologyTest {

    @Test
    void constants_whenAccountEventsAreDefined_shouldExposeStableQueuesRoutingKeysAndEventTypes() {
        assertThat(MessagingTopology.EXCHANGE_NAME).isEqualTo("wallet-hub.events");
        assertThat(MessagingTopology.ACCOUNT_RECORDED_QUEUE).isEqualTo("insights.account-recorded.v1");
        assertThat(MessagingTopology.ACCOUNT_REMOVED_QUEUE).isEqualTo("insights.account-removed.v1");
        assertThat(MessagingTopology.ACCOUNT_PERSISTED_QUEUE).isEqualTo(MessagingTopology.ACCOUNT_RECORDED_QUEUE);
        assertThat(MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY).isEqualTo("account.recorded.v1");
        assertThat(MessagingTopology.ACCOUNT_REMOVED_ROUTING_KEY).isEqualTo("account.removed.v1");
        assertThat(MessagingTopology.ACCOUNT_PERSISTED_ROUTING_KEY).isEqualTo(MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY);
        assertThat(EventTypes.ACCOUNT_RECORDED).isEqualTo("AccountRecorded");
        assertThat(EventTypes.ACCOUNT_REMOVED).isEqualTo("AccountRemoved");
    }

    @Test
    void constants_whenTransactionEventsAreDefined_shouldExposeStableQueuesRoutingKeysAndEventTypes() {
        assertThat(MessagingTopology.TRANSACTION_RECORDED_QUEUE).isEqualTo("insights.transaction-recorded.v1");
        assertThat(MessagingTopology.TRANSACTION_REMOVED_QUEUE).isEqualTo("insights.transaction-removed.v1");
        assertThat(MessagingTopology.BUDGETING_TRANSACTION_RECORDED_QUEUE).isEqualTo("budgeting.transaction-recorded.v1");
        assertThat(MessagingTopology.TRANSACTION_PERSISTED_QUEUE).isEqualTo(MessagingTopology.TRANSACTION_RECORDED_QUEUE);
        assertThat(MessagingTopology.BUDGETING_TRANSACTION_PERSISTED_QUEUE)
                .isEqualTo(MessagingTopology.BUDGETING_TRANSACTION_RECORDED_QUEUE);
        assertThat(MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY).isEqualTo("transaction.recorded.v1");
        assertThat(MessagingTopology.TRANSACTION_REMOVED_ROUTING_KEY).isEqualTo("transaction.removed.v1");
        assertThat(MessagingTopology.TRANSACTION_PERSISTED_ROUTING_KEY)
                .isEqualTo(MessagingTopology.TRANSACTION_RECORDED_ROUTING_KEY);
        assertThat(EventTypes.TRANSACTION_RECORDED).isEqualTo("TransactionRecorded");
        assertThat(EventTypes.TRANSACTION_REMOVED).isEqualTo("TransactionRemoved");
    }
}
