package com.batu.transaction_service.unit.messaging;

import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.event.AccountRemoved;
import com.batu.transaction_service.messaging.AccountRemovedEventListener;
import com.batu.transaction_service.service.TransactionService;

@ExtendWith(MockitoExtension.class)
class AccountRemovedEventListenerTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("e4000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("e4000000-0000-0000-0000-000000000002");
    private static final UUID CONNECTION_ID = UUID.fromString("e4000000-0000-0000-0000-000000000003");

    @Mock
    private TransactionService transactionService;

    @Test
    void onAccountRemoved_whenEventArrives_shouldDeactivateTransactionsByAccount() {
        AccountRemovedEventListener listener = new AccountRemovedEventListener(transactionService);
        BaseEvent<AccountRemoved> event = BaseEvent.create(
                EventTypes.ACCOUNT_REMOVED,
                "account-service",
                "account",
                ACCOUNT_ID,
                new AccountRemoved(ACCOUNT_ID, USER_ID, CONNECTION_ID));

        listener.onAccountRemoved(event);

        verify(transactionService).deactivateTransactionsByAccountId(ACCOUNT_ID);
    }
}
