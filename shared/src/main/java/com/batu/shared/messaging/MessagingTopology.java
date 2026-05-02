package com.batu.shared.messaging;

public final class MessagingTopology {

    public static final String EXCHANGE_NAME = "wallet-hub.events";

    public static final String ACCOUNT_OBSERVED_QUEUE = "account-service.account-observed.v1";
    public static final String CONNECTION_REMOVED_ACCOUNT_QUEUE = "account-service.connection-removed.v1";
    public static final String ACCOUNT_RECORDED_QUEUE = "insights.account-recorded.v1";
    public static final String ACCOUNT_REMOVED_TRANSACTION_QUEUE = "transaction-service.account-removed.v1";
    public static final String ACCOUNT_PERSISTED_QUEUE = ACCOUNT_RECORDED_QUEUE;
    public static final String TRANSACTION_OBSERVED_QUEUE = "transaction-service.transaction-observed.v1";
    public static final String TRANSACTION_RECORDED_QUEUE = "insights.transaction-recorded.v1";
    public static final String BUDGETING_TRANSACTION_RECORDED_QUEUE = "budgeting.transaction-recorded.v1";
    public static final String TRANSACTION_PERSISTED_QUEUE = TRANSACTION_RECORDED_QUEUE;
    public static final String BUDGETING_TRANSACTION_PERSISTED_QUEUE = BUDGETING_TRANSACTION_RECORDED_QUEUE;
    public static final String ACCOUNT_SYNC_QUEUE = ACCOUNT_OBSERVED_QUEUE;
    public static final String TRANSACTION_SYNC_QUEUE = TRANSACTION_OBSERVED_QUEUE;
    public static final String ACCOUNT_DEACTIVATE_QUEUE = CONNECTION_REMOVED_ACCOUNT_QUEUE;
    public static final String TRANSACTION_DEACTIVATE_BY_ACCOUNT_QUEUE = "transactions.deactivate-by-account.v1";

    public static final String ACCOUNT_OBSERVED_ROUTING_KEY = "account.observed.v1";
    public static final String ACCOUNT_RECORDED_ROUTING_KEY = "account.recorded.v1";
    public static final String ACCOUNT_REMOVED_ROUTING_KEY = "account.removed.v1";
    public static final String CONNECTION_REMOVED_ROUTING_KEY = "connection.removed.v1";
    public static final String ACCOUNT_PERSISTED_ROUTING_KEY = ACCOUNT_RECORDED_ROUTING_KEY;
    public static final String TRANSACTION_OBSERVED_ROUTING_KEY = "transaction.observed.v1";
    public static final String TRANSACTION_RECORDED_ROUTING_KEY = "transaction.recorded.v1";
    public static final String TRANSACTION_REMOVED_ROUTING_KEY = "transaction.removed.v1";
    public static final String TRANSACTION_PERSISTED_ROUTING_KEY = TRANSACTION_RECORDED_ROUTING_KEY;
    public static final String ACCOUNT_SYNC_ROUTING_KEY = ACCOUNT_OBSERVED_ROUTING_KEY;
    public static final String TRANSACTION_SYNC_ROUTING_KEY = TRANSACTION_OBSERVED_ROUTING_KEY;
    public static final String ACCOUNT_DEACTIVATE_ROUTING_KEY = CONNECTION_REMOVED_ROUTING_KEY;
    public static final String TRANSACTION_DEACTIVATE_BY_ACCOUNT_ROUTING_KEY = "transactions.deactivate-by-account.v1";

    private MessagingTopology() {
    }
}
