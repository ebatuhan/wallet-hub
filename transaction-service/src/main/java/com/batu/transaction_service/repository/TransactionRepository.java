package com.batu.transaction_service.repository;

import com.batu.transaction_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;


public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByTransactionIdAndUserIdAndIsActiveTrue(UUID transactionId, UUID userId);
    Optional<Transaction> findByExternalId(String externalId);

    @Query(value = """
        INSERT INTO transactions (
            transaction_id,
            user_id,
            account_id,
            external_id,
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
        )
        VALUES (
            gen_random_uuid(),
            :userId,
            :accountId,
            :externalId,
            :amount,
            :isoCurrencyCode,
            :transactionName,
            :transactionType,
            :date,
            :pending,
            :paymentChannel,
            :detailedCategoryId,
            :isActive,
            CURRENT_TIMESTAMP,
            CURRENT_TIMESTAMP
        )
        ON CONFLICT (external_id)
        DO UPDATE SET
            user_id = EXCLUDED.user_id,
            account_id = EXCLUDED.account_id,
            amount = EXCLUDED.amount,
            iso_currency_code = EXCLUDED.iso_currency_code,
            transaction_name = EXCLUDED.transaction_name,
            transaction_type = EXCLUDED.transaction_type,
            date = EXCLUDED.date,
            is_pending = EXCLUDED.is_pending,
            payment_channel = EXCLUDED.payment_channel,
            detailed_category_id = EXCLUDED.detailed_category_id,
            is_active = EXCLUDED.is_active,
            updated_at = CURRENT_TIMESTAMP
        RETURNING *
        """, nativeQuery = true)
    Transaction upsertTransaction(
            @Param("userId") UUID userId,
            @Param("accountId") UUID accountId,
            @Param("externalId") String externalId,
            @Param("amount") BigDecimal amount,
            @Param("isoCurrencyCode") String isoCurrencyCode,
            @Param("transactionName") String transactionName,
            @Param("transactionType") String transactionType,
            @Param("date") LocalDate date,
            @Param("pending") Boolean pending,
            @Param("paymentChannel") String paymentChannel,
            @Param("detailedCategoryId") UUID detailedCategoryId,
            @Param("isActive") boolean isActive
    );
}
