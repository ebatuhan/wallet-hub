package com.batu.saga_orchestration.entity;

public enum LinkSagaStatus {
    STARTED,
    ACCOUNT_PENDING,
    ACCOUNT_FAILED,
    ACCOUNT_PERSISTED,
    TRANSACTION_PENDING,
    TRANSACTION_FAILED,
    TRANSACTION_PERSISTED,
    COMPLETED
}
