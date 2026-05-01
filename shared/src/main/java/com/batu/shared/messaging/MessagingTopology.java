package com.batu.shared.messaging;

public final class MessagingTopology {

    public static final String EXCHANGE_NAME = "analytics.events";

    public static final String ACCOUNT_PERSISTED_QUEUE = "analytics.account.persisted.v1";
    public static final String TRANSACTION_PERSISTED_QUEUE = "analytics.transaction.persisted.v1";
    public static final String BUDGETING_TRANSACTION_PERSISTED_QUEUE = "budgeting.transaction.persisted.v1";
    public static final String ACCOUNT_SYNC_QUEUE = "accounts.sync.v1";
    public static final String TRANSACTION_SYNC_QUEUE = "transactions.sync.v1";
    public static final String ACCOUNT_DEACTIVATE_QUEUE = "accounts.deactivate.v1";
    public static final String TRANSACTION_DEACTIVATE_BY_ACCOUNT_QUEUE = "transactions.deactivate-by-account.v1";

    public static final String ACCOUNT_PERSISTED_ROUTING_KEY = "analytics.account.persisted.v1";
    public static final String TRANSACTION_PERSISTED_ROUTING_KEY = "analytics.transaction.persisted.v1";
    public static final String ACCOUNT_SYNC_ROUTING_KEY = "accounts.sync.v1";
    public static final String TRANSACTION_SYNC_ROUTING_KEY = "transactions.sync.v1";
    public static final String ACCOUNT_DEACTIVATE_ROUTING_KEY = "accounts.deactivate.v1";
    public static final String TRANSACTION_DEACTIVATE_BY_ACCOUNT_ROUTING_KEY = "transactions.deactivate-by-account.v1";

    private MessagingTopology() {
    }
}
