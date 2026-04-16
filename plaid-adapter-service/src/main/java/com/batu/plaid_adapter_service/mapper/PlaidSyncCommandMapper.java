package com.batu.plaid_adapter_service.mapper;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.shared.messaging.command.AccountSyncCommand;
import com.batu.shared.messaging.command.TransactionSyncCommand;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.Transaction;

@Component
public class PlaidSyncCommandMapper {

public AccountSyncCommand toAccountCommand(Connection connection, UUID accountId, AccountBase account) {
    return new AccountSyncCommand(
            accountId,
            connection.getUserId(),
            connection.getInstitutionName(),
            account.getName(),
            account.getType().getValue(),
            account.getSubtype().getValue(),
            account.getMask(),
            BigDecimal.valueOf(account.getBalances().getCurrent()),
            account.getBalances().getAvailable() == null
                    ? BigDecimal.ZERO
                    : BigDecimal.valueOf(account.getBalances().getAvailable()),
            account.getBalances().getIsoCurrencyCode(),
            true
    );
}
    public AccountSyncCommand toDeactivateAccountCommand(Connection connection, UUID accountId) {
        return new AccountSyncCommand(
                accountId,
                connection.getUserId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false);
    }

    public TransactionSyncCommand toTransactionCommand(Connection connection, UUID transactionId, UUID accountId,
            Transaction transaction) {
        return new TransactionSyncCommand(
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
                transaction.getPersonalFinanceCategory().getDetailed(),
                true);
    }

    public TransactionSyncCommand toDeactivateTransactionCommand(Connection connection, UUID transactionId) {
        return new TransactionSyncCommand(
                transactionId,
                connection.getUserId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false);
    }
}
