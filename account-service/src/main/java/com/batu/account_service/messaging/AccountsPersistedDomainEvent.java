package com.batu.account_service.messaging;

import java.util.List;

import com.batu.shared.messaging.event.AccountPersistedEvent;

public record AccountsPersistedDomainEvent(List<AccountPersistedEvent> accounts) {
}
