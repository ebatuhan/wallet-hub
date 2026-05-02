package com.batu.shared.messaging;

public final class EventTypes {
    public static final String ACCOUNT_OBSERVED = "AccountObserved";
    public static final String ACCOUNT_RECORDED = "AccountRecorded";
    public static final String ACCOUNT_REMOVED = "AccountRemoved";
    public static final String CONNECTION_REMOVED = "ConnectionRemoved";
    public static final String TRANSACTION_OBSERVED = "TransactionObserved";
    public static final String TRANSACTION_RECORDED = "TransactionRecorded";
    public static final String TRANSACTION_REMOVED = "TransactionRemoved";

    private EventTypes() {
    }
}
