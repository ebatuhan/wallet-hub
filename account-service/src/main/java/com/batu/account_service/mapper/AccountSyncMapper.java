package com.batu.account_service.mapper;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batu.account_service.entity.Account;
import com.batu.shared.messaging.command.AccountSyncCommand;
import com.batu.shared.messaging.event.AccountPersistedEvent;

@Component
public class AccountSyncMapper {

    public Account toEntity(AccountSyncCommand command) {
        return new Account(
                command.getAccountId(),
                command.getUserId(),
                command.getInstitutionName(),
                command.getAccountName(),
                command.getAccountType(),
                command.getAccountSubtype(),
                command.getAccountMask(),
                command.getCurrentBalance(),
                command.getAvailableBalance(),
                command.getIsoCurrencyCode(),
                command.isActive());
    }

    public AccountPersistedEvent toPersistedEvent(AccountSyncCommand command) {
        return new AccountPersistedEvent(
                UUID.randomUUID(),
                Instant.now(),
                "account-service",
                command.getAccountId(),
                command.getUserId(),
                command.getInstitutionName(),
                command.getAccountName(),
                command.getAccountType(),
                command.getAccountSubtype(),
                command.getAccountMask(),
                command.getCurrentBalance(),
                command.getAvailableBalance(),
                command.getIsoCurrencyCode(),
                command.isActive());
    }

    public AccountPersistedEvent toPersistedEvent(Account account) {
        return new AccountPersistedEvent(
                UUID.randomUUID(),
                Instant.now(),
                "account-service",
                account.getAccountId(),
                account.getUserId(),
                account.getInstitutionName(),
                account.getAccountName(),
                account.getAccountType(),
                account.getAccountSubtype(),
                account.getAccountMask(),
                account.getCurrentBalance(),
                account.getAvailableBalance(),
                account.getIsoCurrencyCode(),
                account.isActive());
    }
}
