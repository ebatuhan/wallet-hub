package com.batu.plaid_adapter_service.unit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.mapper.PlaidRequestMapper;
import com.batu.shared.messaging.event.AccountObserved;
import com.batu.shared.messaging.event.TransactionObserved;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountBalance;
import com.plaid.client.model.AccountSubtype;
import com.plaid.client.model.PersonalFinanceCategory;
import com.plaid.client.model.Transaction;

class PlaidRequestMapperTest {

    private final PlaidRequestMapper mapper = new PlaidRequestMapper();

    @Test
    void toAccountObserved_mapsPlaidAccountFields() {
        Connection connection = new Connection(UUID.randomUUID(), "item-1", "access-token", "ins-1", "Test Bank");
        UUID accountId = UUID.randomUUID();
        AccountBase account = new AccountBase()
                .accountId("plaid-account-1")
                .name("Primary Checking")
                .type(com.plaid.client.model.AccountType.DEPOSITORY)
                .subtype(AccountSubtype.CHECKING)
                .mask("0000")
                .balances(new AccountBalance()
                        .current(125.50)
                        .available(100.25)
                        .isoCurrencyCode("USD"));

        AccountObserved request = mapper.toAccountObserved(connection, accountId, account);

        assertEquals(accountId, request.getAccountId());
        assertEquals(connection.getUserId(), request.getUserId());
        assertEquals(connection.getConnectionId(), request.getConnectionId());
        assertEquals("Test Bank", request.getInstitutionName());
        assertEquals("Primary Checking", request.getAccountName());
        assertEquals("depository", request.getAccountType());
        assertEquals("checking", request.getAccountSubtype());
        assertEquals("0000", request.getAccountMask());
        assertEquals(0, BigDecimal.valueOf(125.50).compareTo(request.getCurrentBalance()));
        assertEquals(0, BigDecimal.valueOf(100.25).compareTo(request.getAvailableBalance()));
        assertEquals("USD", request.getIsoCurrencyCode());
    }

    @Test
    void toTransactionObserved_mapsPlaidTransactionFields() {
        Connection connection = new Connection(UUID.randomUUID(), "item-1", "access-token", "ins-1", "Test Bank");
        UUID transactionId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        Transaction transaction = new Transaction()
                .transactionId("plaid-transaction-1")
                .accountId("plaid-account-1")
                .amount(15.75)
                .isoCurrencyCode("USD")
                .name("Coffee Shop")
                .transactionType(Transaction.TransactionTypeEnum.PLACE)
                .date(LocalDate.of(2026, 4, 17))
                .pending(false)
                .paymentChannel(Transaction.PaymentChannelEnum.IN_STORE)
                .personalFinanceCategory(new PersonalFinanceCategory().detailed("FOOD_AND_DRINK_COFFEE"));

        TransactionObserved request = mapper.toTransactionObserved(connection, transactionId, accountId, transaction);

        assertEquals(transactionId, request.getTransactionId());
        assertEquals(connection.getUserId(), request.getUserId());
        assertEquals(accountId, request.getAccountId());
        assertEquals(0, BigDecimal.valueOf(15.75).compareTo(request.getAmount()));
        assertEquals("USD", request.getIsoCurrencyCode());
        assertEquals("Coffee Shop", request.getTransactionName());
        assertEquals("place", request.getTransactionType());
        assertEquals(LocalDate.of(2026, 4, 17), request.getDate());
        assertFalse(request.getPending());
        assertEquals("in store", request.getPaymentChannel());
        assertEquals("FOOD_AND_DRINK_COFFEE", request.getDetailedCategoryCode());
        assertTrue(request.isActive());
    }

}
