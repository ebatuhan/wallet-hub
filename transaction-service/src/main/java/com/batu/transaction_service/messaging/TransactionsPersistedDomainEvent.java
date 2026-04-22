package com.batu.transaction_service.messaging;

import java.util.List;

import com.batu.shared.messaging.event.TransactionPersistedEvent;

public record TransactionsPersistedDomainEvent(List<TransactionPersistedEvent> transactions) {
}
