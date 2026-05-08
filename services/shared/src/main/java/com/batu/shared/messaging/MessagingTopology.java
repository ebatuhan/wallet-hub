package com.batu.shared.messaging;

public final class MessagingTopology {

    public static final String EXCHANGE_NAME = "wallet-hub.events";

    public static final String ACCOUNT_RECORDED_QUEUE = "insights.account-recorded.v1";
    public static final String ACCOUNT_REMOVED_QUEUE = "insights.account-removed.v1";
    public static final String ACCOUNT_PERSISTED_QUEUE = ACCOUNT_RECORDED_QUEUE;
    public static final String TRANSACTION_RECORDED_QUEUE = "insights.transaction-recorded.v1";
    public static final String TRANSACTION_REMOVED_QUEUE = "insights.transaction-removed.v1";
    public static final String TRANSACTION_ACCOUNT_REMOVED_QUEUE = "transactions.account-removed.v1";
    public static final String BUDGETING_TRANSACTION_RECORDED_QUEUE = "budgeting.transaction-recorded.v1";
    public static final String TRANSACTION_PERSISTED_QUEUE = TRANSACTION_RECORDED_QUEUE;
    public static final String BUDGETING_TRANSACTION_PERSISTED_QUEUE = BUDGETING_TRANSACTION_RECORDED_QUEUE;

    public static final String ACCOUNT_RECORDED_ROUTING_KEY = "account.recorded.v1";
    public static final String ACCOUNT_REMOVED_ROUTING_KEY = "account.removed.v1";
    public static final String ACCOUNT_PERSISTED_ROUTING_KEY = ACCOUNT_RECORDED_ROUTING_KEY;
    public static final String TRANSACTION_RECORDED_ROUTING_KEY = "transaction.recorded.v1";
    public static final String TRANSACTION_REMOVED_ROUTING_KEY = "transaction.removed.v1";
    public static final String TRANSACTION_PERSISTED_ROUTING_KEY = TRANSACTION_RECORDED_ROUTING_KEY;

    private MessagingTopology() {
    }
}
