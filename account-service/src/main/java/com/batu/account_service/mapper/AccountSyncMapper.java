package com.batu.account_service.mapper;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batu.account_service.entity.Account;
import com.batu.shared.dto.request.AccountRequestDto;
import com.batu.shared.messaging.event.AccountPersistedEvent;

@Component
public class AccountSyncMapper {

    public Account toEntity(AccountRequestDto request) {
        return new Account(
                request.getAccountId(),
                request.getUserId(),
                request.getConnectionId(),
                request.getInstitutionName(),
                request.getAccountName(),
                request.getAccountType(),
                request.getAccountSubtype(),
                request.getAccountMask(),
                request.getCurrentBalance(),
                request.getAvailableBalance(),
                request.getIsoCurrencyCode(),
                request.isActive());
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
