package com.batu.transaction_service.repository;

import com.batu.transaction_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface TransactionRepository extends JpaRepository<Transaction, UUID>, JpaSpecificationExecutor<Transaction> {
    Optional<Transaction> findByTransactionIdAndUserIdAndIsActiveTrue(UUID transactionId, UUID userId);

    List<Transaction> findByAccountIdInAndIsActiveTrue(Collection<UUID> accountIds);

    @Query("""
            select transaction
            from Transaction transaction
            left join fetch transaction.detailedCategory detailedCategory
            left join fetch detailedCategory.transactionPrimaryCategory primaryCategory
            where transaction.transactionId in :transactionIds
            """)
    List<Transaction> findAllByTransactionIdInWithCategory(@Param("transactionIds") Collection<UUID> transactionIds);

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
