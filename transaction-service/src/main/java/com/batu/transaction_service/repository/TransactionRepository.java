package com.batu.transaction_service.repository;

import com.batu.transaction_service.entity.Transaction;
import com.batu.transaction_service.entity.TransactionDetailedCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;


public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByTransactionIdAndUserIdAndIsActiveTrue(UUID transactionId, UUID userId);

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
                :isActive,
                current_timestamp,
                current_timestamp
            )
            """, nativeQuery = true)
    int insertSyncedTransaction(
            @Param("transactionId") UUID transactionId,
            @Param("userId") UUID userId,
            @Param("accountId") UUID accountId,
            @Param("amount") BigDecimal amount,
            @Param("isoCurrencyCode") String isoCurrencyCode,
            @Param("transactionName") String transactionName,
            @Param("transactionType") String transactionType,
            @Param("date") LocalDate date,
            @Param("pending") Boolean pending,
            @Param("paymentChannel") String paymentChannel,
            @Param("detailedCategoryId") UUID detailedCategoryId,
            @Param("isActive") boolean isActive);

    @Modifying
    @Query("""
            update Transaction transaction
            set transaction.userId = :userId,
                transaction.accountId = :accountId,
                transaction.amount = :amount,
                transaction.isoCurrencyCode = :isoCurrencyCode,
                transaction.transactionName = :transactionName,
                transaction.transactionType = :transactionType,
                transaction.date = :date,
                transaction.pending = :pending,
                transaction.paymentChannel = :paymentChannel,
                transaction.detailedCategory = :detailedCategory,
                transaction.isActive = :isActive
            where transaction.transactionId = :transactionId
            """)
    int updateSyncedTransaction(
            @Param("transactionId") UUID transactionId,
            @Param("userId") UUID userId,
            @Param("accountId") UUID accountId,
            @Param("amount") BigDecimal amount,
            @Param("isoCurrencyCode") String isoCurrencyCode,
            @Param("transactionName") String transactionName,
            @Param("transactionType") String transactionType,
            @Param("date") LocalDate date,
            @Param("pending") Boolean pending,
            @Param("paymentChannel") String paymentChannel,
            @Param("detailedCategory") TransactionDetailedCategory detailedCategory,
            @Param("isActive") boolean isActive);

    @Modifying
    @Query("""
            update Transaction transaction
            set transaction.isActive = false
            where transaction.transactionId = :transactionId
              and transaction.userId = :userId
            """)
    int deactivateSyncedTransaction(
            @Param("transactionId") UUID transactionId,
            @Param("userId") UUID userId);
}
