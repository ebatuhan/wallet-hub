package com.batu.shared.messaging;

public final class MessagingTopology {

    public static final String EXCHANGE_NAME = "analytics.events";

    public static final String ACCOUNT_PERSISTED_QUEUE = "analytics.account.persisted.v1";
    public static final String TRANSACTION_PERSISTED_QUEUE = "analytics.transaction.persisted.v1";

    public static final String ACCOUNT_PERSISTED_ROUTING_KEY = "analytics.account.persisted.v1";
    public static final String TRANSACTION_PERSISTED_ROUTING_KEY = "analytics.transaction.persisted.v1";

    private MessagingTopology() {
    }
}
