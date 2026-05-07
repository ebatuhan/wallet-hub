package com.batu.transaction_service.repository;

import java.util.UUID;

import com.batu.shared.dto.request.TransactionUpsertRequestDto;
import com.batu.transaction_service.entity.Transaction;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Transaction upsertTransaction(TransactionUpsertRequestDto request, UUID detailedCategoryId) {
        String sql = """
                insert into transactions (
                    transaction_id,
                    user_id,
                    account_id,
                    amount,
                    iso_currency_code,
                    transaction_name,
                    transaction_type,
                    date,
                    is_pending,
                    payment_channel,
                    detailed_category_id,
                    is_active,
                    created_at,
                    updated_at
                ) values (
                    :transactionId,
                    :userId,
                    :accountId,
                    :amount,
                    :isoCurrencyCode,
                    :transactionName,
                    :transactionType,
                    :date,
                    :pending,
                    :paymentChannel,
                    :detailedCategoryId,
                    :active,
                    now(),
                    now()
                )
                on conflict (transaction_id) do update set
                    user_id = excluded.user_id,
                    account_id = excluded.account_id,
                    amount = excluded.amount,
                    iso_currency_code = excluded.iso_currency_code,
                    transaction_name = excluded.transaction_name,
                    transaction_type = excluded.transaction_type,
                    date = excluded.date,
                    is_pending = excluded.is_pending,
                    payment_channel = excluded.payment_channel,
                    detailed_category_id = excluded.detailed_category_id,
                    is_active = excluded.is_active,
                    updated_at = now()
                returning *
                """;

        return (Transaction) entityManager.createNativeQuery(sql, Transaction.class)
                .setParameter("transactionId", request.getTransactionId())
                .setParameter("userId", request.getUserId())
                .setParameter("accountId", request.getAccountId())
                .setParameter("amount", request.getAmount())
                .setParameter("isoCurrencyCode", request.getIsoCurrencyCode())
                .setParameter("transactionName", request.getTransactionName())
                .setParameter("transactionType", request.getTransactionType())
                .setParameter("date", request.getDate())
                .setParameter("pending", request.getPending())
                .setParameter("paymentChannel", request.getPaymentChannel())
                .setParameter("detailedCategoryId", detailedCategoryId)
                .setParameter("active", request.isActive())
                .getSingleResult();
    }
}
