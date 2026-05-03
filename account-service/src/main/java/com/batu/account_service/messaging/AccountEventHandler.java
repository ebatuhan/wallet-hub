package com.batu.account_service.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.batu.account_service.entity.Account;
import com.batu.account_service.service.AccountService;
import com.batu.account_service.service.input.RecordAccountInput;
import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;
import com.batu.shared.messaging.event.AccountObserved;
import com.batu.shared.messaging.event.AccountRecorded;
import com.batu.shared.messaging.event.AccountRemoved;
import com.batu.shared.messaging.event.ConnectionRemoved;

@Component
public class AccountEventHandler {
    private static final String SOURCE = "account-service";
    private static final String ACCOUNT_AGGREGATE = "account";

    private final AccountService accountService;
    private final AccountInbox accountInbox;
    private final AccountOutbox accountOutbox;

    public AccountEventHandler(AccountService accountService, AccountInbox accountInbox, AccountOutbox accountOutbox) {
        this.accountService = accountService;
        this.accountInbox = accountInbox;
        this.accountOutbox = accountOutbox;
    }

    @Transactional
    public void handleAccountObserved(BaseEvent<AccountObserved> event) {
        accountInbox.process(event, () -> accountService.recordAccount(toInput(event))
                .ifPresent(account -> saveAccountRecorded(account, event)));
    }

    @Transactional
    public void handleConnectionRemoved(BaseEvent<ConnectionRemoved> event) {
        accountInbox.process(event, () -> accountService
                .deactivateByConnection(event.getPayload().getConnectionId(), event.getAggregateVersion())
                .forEach(account -> saveAccountRemoved(account, event)));
    }

    private RecordAccountInput toInput(BaseEvent<AccountObserved> event) {
        AccountObserved payload = event.getPayload();
        return new RecordAccountInput(
                payload.getAccountId(),
                payload.getUserId(),
                payload.getConnectionId(),
                payload.getInstitutionName(),
                payload.getAccountName(),
                payload.getAccountType(),
                payload.getAccountSubtype(),
                payload.getAccountMask(),
                payload.getCurrentBalance(),
                payload.getAvailableBalance(),
                payload.getIsoCurrencyCode(),
                event.getAggregateVersion());
    }

    private void saveAccountRecorded(Account account, BaseEvent<?> cause) {
        AccountRecorded payload = new AccountRecorded(
                account.getAccountId(),
                account.getUserId(),
                account.getConnectionId(),
                account.getInstitutionName(),
                account.getAccountName(),
                account.getAccountType(),
                account.getAccountSubtype(),
                account.getAccountMask(),
                account.getCurrentBalance(),
                account.getAvailableBalance(),
                account.getIsoCurrencyCode(),
                account.isActive());

        accountOutbox.save(MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY, BaseEvent.causedBy(
                EventTypes.ACCOUNT_RECORDED,
                SOURCE,
                ACCOUNT_AGGREGATE,
                account.getAccountId(),
                account.getSyncVersion(),
                payload,
                cause));
    }

    private void saveAccountRemoved(Account account, BaseEvent<?> cause) {
        AccountRemoved payload = new AccountRemoved(
                account.getAccountId(),
                account.getUserId(),
                account.getConnectionId());

        accountOutbox.save(MessagingTopology.ACCOUNT_REMOVED_ROUTING_KEY, BaseEvent.causedBy(
                EventTypes.ACCOUNT_REMOVED,
                SOURCE,
                ACCOUNT_AGGREGATE,
                account.getAccountId(),
                account.getSyncVersion(),
                payload,
                cause));
    }
}
