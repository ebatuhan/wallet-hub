package com.batu.shared.messaging.saga;

public final class SagaChannels {

    private SagaChannels() {
    }

    public static final String EXCHANGE = "saga.exchange";


    public static final String START_LINK_SAGA_QUEUE = "link.start.command";
    public static final String START_LINK_SAGA_KEY = "link.start.command";

    public static final String FETCH_ACCOUNTS_COMMAND_QUEUE = "fetch.accounts.command";
    public static final String FETCH_ACCOUNTS_EVENT_QUEUE = "fetch.accounts.event";
    public static final String FETCH_ACCOUNTS_COMPENSATE_QUEUE = "fetch.accounts.compensate";

    public static final String FETCH_ACCOUNTS_COMMAND_KEY = "fetch.accounts.command";
    public static final String FETCH_ACCOUNTS_EVENT_KEY = "fetch.accounts.event";
    public static final String FETCH_ACCOUNTS_COMPENSATE_KEY = "fetch.accounts.compensate";

    public static final String PERSIST_ACCOUNTS_COMMAND_QUEUE = "persist.accounts.command";
    public static final String PERSIST_ACCOUNTS_EVENT_QUEUE = "persist.accounts.event";

    public static final String PERSIST_ACCOUNTS_COMMAND_KEY = "persist.accounts.command";
    public static final String PERSIST_ACCOUNTS_EVENT_KEY = "persist.accounts.event";

    public static final String FETCH_TRANSACTIONS_COMMAND_QUEUE = "fetch.transactions.command";
    public static final String FETCH_TRANSACTIONS_EVENT_QUEUE = "fetch.transactions.event";
    public static final String FETCH_TRANSACTIONS_COMPENSATE_QUEUE = "fetch.transactions.compensate";

    public static final String FETCH_TRANSACTIONS_COMMAND_KEY = "fetch.transactions.command";
    public static final String FETCH_TRANSACTIONS_EVENT_KEY = "fetch.transactions.event";
    public static final String FETCH_TRANSACTIONS_COMPENSATE_KEY = "fetch.transactions.compensate";
}