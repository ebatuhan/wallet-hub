package com.batu.transaction_service.repository;

import com.batu.transaction_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByTransactionIdAndUserIdAndIsActiveTrue(UUID transactionId, UUID userId);

    List<Transaction> findByAccountIdAndIsActiveTrue(UUID accountId);

    @Query("""
            select transaction
            from Transaction transaction
            join fetch transaction.detailedCategory detailedCategory
            join fetch detailedCategory.transactionPrimaryCategory primaryCategory
            where transaction.accountId = :accountId
              and transaction.syncVersion = :syncVersion
            """)
    List<Transaction> findByAccountIdAndSyncVersionWithCategory(@Param("accountId") UUID accountId,
            @Param("syncVersion") long syncVersion);

    @Query("""
            select transaction
            from Transaction transaction
            join fetch transaction.detailedCategory detailedCategory
            join fetch detailedCategory.transactionPrimaryCategory primaryCategory
            where transaction.transactionId = :transactionId
              and transaction.userId = :userId
            """)
    Optional<Transaction> findByTransactionIdAndUserIdWithCategory(
            @Param("transactionId") UUID transactionId,
            @Param("userId") UUID userId);

    @Modifying
    @Query(value = """
            insert into transactions (transaction_id, user_id, account_id, amount, iso_currency_code,
                                      transaction_name, transaction_type, date, is_pending, payment_channel,
                                      detailed_category_id, is_active, sync_version, created_at, updated_at)
            values (:transactionId, :userId, :accountId, :amount, :isoCurrencyCode,
                    :transactionName, :transactionType, :date, :pending, :paymentChannel,
                    :detailedCategoryId, :active, :syncVersion, current_timestamp, current_timestamp)
            on conflict (transaction_id) do update
            set user_id = excluded.user_id,
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
                sync_version = excluded.sync_version,
                updated_at = current_timestamp
            where transactions.sync_version <= excluded.sync_version
            """, nativeQuery = true)
    int upsertFromSync(@Param("transactionId") UUID transactionId,
            @Param("userId") UUID userId,
            @Param("accountId") UUID accountId,
            @Param("amount") BigDecimal amount,
            @Param("isoCurrencyCode") String isoCurrencyCode,
            @Param("transactionName") String transactionName,
            @Param("transactionType") String transactionType,
            @Param("date") java.time.LocalDate date,
            @Param("pending") Boolean pending,
            @Param("paymentChannel") String paymentChannel,
            @Param("detailedCategoryId") UUID detailedCategoryId,
            @Param("active") boolean active,
            @Param("syncVersion") long syncVersion);

    @Modifying
    @Query(value = """
            update transactions
            set is_active = false,
                sync_version = :syncVersion,
                updated_at = current_timestamp
            where account_id = :accountId
              and is_active = true
              and sync_version <= :syncVersion
            """, nativeQuery = true)
    int deactivateByAccountIdFromSync(@Param("accountId") UUID accountId,
            @Param("syncVersion") long syncVersion);
}
