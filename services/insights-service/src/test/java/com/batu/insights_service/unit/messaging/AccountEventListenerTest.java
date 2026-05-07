package com.batu.insights_service.unit.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.batu.insights_service.entity.AccountBalanceDataPointRow;
import com.batu.insights_service.messaging.AccountEventListener;
import com.batu.insights_service.messaging.InsightsInbox;
import com.batu.insights_service.service.AccountInsightsService;
import com.batu.insights_service.service.TransactionInsightsService;
import com.batu.shared.messaging.BaseEvent;
import com.batu.shared.messaging.event.AccountRecorded;
import com.batu.shared.messaging.event.AccountRemoved;

@ExtendWith(MockitoExtension.class)
class AccountEventListenerTest {

    private static final UUID EVENT_ID = UUID.fromString("87000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("87000000-0000-0000-0000-000000000002");
    private static final UUID ACCOUNT_ID = UUID.fromString("87000000-0000-0000-0000-000000000003");
    private static final UUID CONNECTION_ID = UUID.fromString("87000000-0000-0000-0000-000000000004");

    @Mock
    private AccountInsightsService accountInsightsService;

    @Mock
    private TransactionInsightsService transactionInsightsService;

    @Mock
    private InsightsInbox insightsInbox;

    @InjectMocks
    private AccountEventListener listener;

    @Test
    void onAccountRecorded_whenEventArrives_shouldProjectBalanceAtUtcEventDate() {
        BaseEvent<AccountRecorded> event = accountRecordedEvent(Instant.parse("2026-04-17T23:30:00Z"));

        listener.onAccountRecorded(event);
        Runnable handler = captureRecordedHandler(event);
        handler.run();

        ArgumentCaptor<AccountBalanceDataPointRow> rowCaptor = ArgumentCaptor.forClass(AccountBalanceDataPointRow.class);
        verify(accountInsightsService).save(rowCaptor.capture());
        AccountBalanceDataPointRow row = rowCaptor.getValue();
        assertThat(row.accountId()).isEqualTo(ACCOUNT_ID);
        assertThat(row.userId()).isEqualTo(USER_ID);
        assertThat(row.balance()).isEqualByComparingTo("125.50");
        assertThat(row.isoCurrencyCode()).isEqualTo("USD");
        assertThat(row.date()).isEqualTo(LocalDate.of(2026, 4, 17));
    }

    @Test
    void onAccountRemoved_whenEventArrives_shouldRemoveAccountAndTransactionsForSameUser() {
        AccountRemoved payload = new AccountRemoved(ACCOUNT_ID, USER_ID, CONNECTION_ID);
        BaseEvent<AccountRemoved> event = new BaseEvent<>(
                EVENT_ID,
                "AccountRemoved",
                "account-service",
                EVENT_ID,
                null,
                "account",
                ACCOUNT_ID,
                Instant.parse("2026-04-17T10:15:30Z"),
                payload);

        listener.onAccountRemoved(event);
        Runnable handler = captureRemovedHandler(event);
        handler.run();

        verify(accountInsightsService).remove(payload);
        verify(transactionInsightsService).removeAccountTransactions(ACCOUNT_ID, USER_ID);
    }

    private Runnable captureRecordedHandler(BaseEvent<AccountRecorded> event) {
        ArgumentCaptor<Runnable> handlerCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(insightsInbox).process(org.mockito.ArgumentMatchers.same(event), handlerCaptor.capture());
        return handlerCaptor.getValue();
    }

    private Runnable captureRemovedHandler(BaseEvent<AccountRemoved> event) {
        ArgumentCaptor<Runnable> handlerCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(insightsInbox).process(org.mockito.ArgumentMatchers.same(event), handlerCaptor.capture());
        return handlerCaptor.getValue();
    }

    private static BaseEvent<AccountRecorded> accountRecordedEvent(Instant occurredAt) {
        AccountRecorded payload = new AccountRecorded(
                ACCOUNT_ID,
                USER_ID,
                CONNECTION_ID,
                "Test Bank",
                "Checking",
                "depository",
                "checking",
                "0000",
                new BigDecimal("125.50"),
                new BigDecimal("100.25"),
                "USD",
                true);
        return new BaseEvent<>(
                EVENT_ID,
                "AccountRecorded",
                "account-service",
                EVENT_ID,
                null,
                "account",
                ACCOUNT_ID,
                occurredAt,
                payload);
    }
}
