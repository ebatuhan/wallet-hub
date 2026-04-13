package com.batu.shared.messaging;

public final class MessagingTopology {

    public static final String EXCHANGE_NAME = "analytics.events";
    public static final String COMMAND_EXCHANGE_NAME = "plaid.sync.commands";

    public static final String ACCOUNT_PERSISTED_QUEUE = "analytics.account.persisted.v1";
    public static final String TRANSACTION_PERSISTED_QUEUE = "analytics.transaction.persisted.v1";
    public static final String BUDGETING_TRANSACTION_PERSISTED_QUEUE = "budgeting.transaction.persisted.v1";
    public static final String ACCOUNT_CREATE_QUEUE = "plaid.account.create.v1";
    public static final String ACCOUNT_UPDATE_QUEUE = "plaid.account.update.v1";
    public static final String TRANSACTION_CREATE_QUEUE = "plaid.transaction.create.v1";
    public static final String TRANSACTION_UPDATE_QUEUE = "plaid.transaction.update.v1";

    public static final String ACCOUNT_PERSISTED_ROUTING_KEY = "analytics.account.persisted.v1";
    public static final String TRANSACTION_PERSISTED_ROUTING_KEY = "analytics.transaction.persisted.v1";
    public static final String ACCOUNT_CREATE_ROUTING_KEY = "plaid.account.create.v1";
    public static final String ACCOUNT_UPDATE_ROUTING_KEY = "plaid.account.update.v1";
    public static final String TRANSACTION_CREATE_ROUTING_KEY = "plaid.transaction.create.v1";
    public static final String TRANSACTION_UPDATE_ROUTING_KEY = "plaid.transaction.update.v1";

    private MessagingTopology() {
    }
}
