package com.batu.transaction_service.repository;

import com.batu.transaction_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction>, TransactionRepositoryCustom {
    Optional<Transaction> findByTransactionIdAndUserIdAndIsActiveTrue(UUID transactionId, UUID userId);

    List<Transaction> findByAccountIdAndIsActiveTrue(UUID accountId);

    @Modifying
    @Query("""
            update Transaction transaction
            set transaction.isActive = false
            where transaction.accountId = :accountId
              and transaction.isActive = true
            """)
    int deactivateActiveTransactionsByAccountId(@Param("accountId") UUID accountId);

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

}
