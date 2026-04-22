package com.batu.budgeting.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.batu.budgeting.entity.Budget;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    List<Budget> findByUserIdAndActiveTrue(UUID userId);

    Optional<Budget> findByIdAndUserId(UUID id, UUID userId);

    List<Budget> findByUserIdAndCategoryIdAndIsoCurrencyCodeAndActiveTrue(UUID userId, UUID categoryId, String isoCurrencyCode);

    @Query("""
            select budget
            from Budget budget
            where budget.userId = :userId
              and budget.categoryId = :categoryId
              and budget.isoCurrencyCode = :currency
              and budget.active = true
              and budget.periodStart <= :txDate
            """)
    List<Budget> findCandidates(
            @Param("userId") UUID userId,
            @Param("categoryId") UUID categoryId,
            @Param("currency") String currency,
            @Param("txDate") LocalDate txDate);
}
