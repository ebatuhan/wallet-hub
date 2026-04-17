package com.batu.plaid_adapter_service.mapper;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.service.PlaidInternalIdGenerator;
import com.batu.shared.dto.request.AccountRequestDto;
import com.batu.shared.dto.request.TransactionRequestDto;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.Transaction;

@Component
public class PlaidSyncCommandMapper {

    private final PlaidInternalIdGenerator plaidInternalIdGenerator;

    public PlaidSyncCommandMapper(PlaidInternalIdGenerator plaidInternalIdGenerator) {
        this.plaidInternalIdGenerator = plaidInternalIdGenerator;
    }

    public AccountRequestDto toAccountRequest(Connection connection, AccountBase account) {
        String accountSubtype = account.getSubtype() == null ? "" : account.getSubtype().getValue();
        String accountMask = account.getMask() == null ? "" : account.getMask();

        return new AccountRequestDto(
                plaidInternalIdGenerator.accountId(connection, account.getAccountId()),
                connection.getConnectionId(),
                connection.getUserId(),
                connection.getInstitutionName(),
                account.getName(),
                account.getType().getValue(),
                accountSubtype,
                accountMask,
                BigDecimal.valueOf(account.getBalances().getCurrent()),
                account.getBalances().getAvailable() == null
                        ? BigDecimal.ZERO
                        : BigDecimal.valueOf(account.getBalances().getAvailable()),
                account.getBalances().getIsoCurrencyCode(),
                true);
    }

    public TransactionRequestDto toTransactionRequest(Connection connection, Transaction transaction) {
        return new TransactionRequestDto(
                plaidInternalIdGenerator.transactionId(connection, transaction.getTransactionId()),
                connection.getUserId(),
                plaidInternalIdGenerator.accountId(connection, transaction.getAccountId()),
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

    public TransactionRequestDto toDeactivateTransactionRequest(Connection connection, String plaidTransactionId) {
        return new TransactionRequestDto(
                plaidInternalIdGenerator.transactionId(connection, plaidTransactionId),
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
