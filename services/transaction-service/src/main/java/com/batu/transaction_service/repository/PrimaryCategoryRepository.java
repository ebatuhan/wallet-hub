package com.batu.transaction_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.batu.transaction_service.entity.TransactionPrimaryCategory;


public interface PrimaryCategoryRepository extends JpaRepository<TransactionPrimaryCategory, UUID> {
    Optional<TransactionPrimaryCategory> findByCategoryCode(String categoryCode);

    List<TransactionPrimaryCategory> findByTransactionPrimaryCategoryIdIn(Set<UUID> ids);

}
