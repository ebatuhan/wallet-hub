package com.batu.plaid_adapter_service.mapper;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.shared.dto.request.AccountRequestDto;
import com.batu.shared.dto.request.TransactionRequestDto;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.Transaction;

@Component
public class PlaidRequestMapper {

    public AccountRequestDto toAccountRequest(Connection connection, UUID accountId, AccountBase account) {
        String accountSubtype = account.getSubtype() == null ? "" : account.getSubtype().getValue();
        String accountMask = account.getMask() == null ? "" : account.getMask();

        return new AccountRequestDto(
                accountId,
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

    public TransactionRequestDto toTransactionRequest(Connection connection, UUID transactionId, UUID accountId,
            Transaction transaction) {
        return new TransactionRequestDto(
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

    public TransactionRequestDto toDeactivateTransactionRequest(Connection connection, UUID transactionId) {
        return new TransactionRequestDto(
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
