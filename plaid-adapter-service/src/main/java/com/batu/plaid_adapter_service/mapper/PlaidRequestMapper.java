package com.batu.plaid_adapter_service.mapper;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.shared.messaging.event.AccountObserved;
import com.batu.shared.messaging.event.TransactionObserved;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.Transaction;

@Component
public class PlaidRequestMapper {

    public AccountObserved toAccountObserved(Connection connection, UUID accountId, AccountBase account) {
        String accountSubtype = account.getSubtype() == null ? "" : account.getSubtype().getValue();
        String accountMask = account.getMask() == null ? "" : account.getMask();

        return new AccountObserved(
                accountId,
                connection.getUserId(),
                connection.getConnectionId(),
                connection.getInstitutionName(),
                account.getName(),
                account.getType().getValue(),
                accountSubtype,
                accountMask,
                BigDecimal.valueOf(account.getBalances().getCurrent()),
                account.getBalances().getAvailable() == null
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(account.getBalances().getAvailable()),
                account.getBalances().getIsoCurrencyCode());
    }

    public TransactionObserved toTransactionObserved(Connection connection, UUID transactionId, UUID accountId,
            Transaction transaction) {
        return new TransactionObserved(
                transactionId,
                connection.getUserId(),
                accountId,
                BigDecimal.valueOf(transaction.getAmount()),
                transaction.getIsoCurrencyCode(),
                transaction.getName(),
                transaction.getTransactionType().getValue(),
                transaction.getDate(),
                transaction.getPending(),
                transaction.getPaymentChannel().getValue(),
                transaction.getPersonalFinanceCategory() == null
                        ? "OTHER_OTHER"
                        : transaction.getPersonalFinanceCategory().getDetailed(),
                true);
    }
}
