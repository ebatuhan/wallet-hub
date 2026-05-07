package com.batu.transaction_service.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.batu.transaction_service.entity.TransactionDetailedCategory;

public interface DetailedCategoryRepository extends JpaRepository<TransactionDetailedCategory, UUID>  {
    Optional<TransactionDetailedCategory> findByCategoryCode(String categoryCode);

    @Query("""
            select detailedCategory
            from TransactionDetailedCategory detailedCategory
            join fetch detailedCategory.transactionPrimaryCategory primaryCategory
            where detailedCategory.categoryCode = :categoryCode
            """)
    Optional<TransactionDetailedCategory> findByCategoryCodeWithPrimaryCategory(@Param("categoryCode") String categoryCode);
}
