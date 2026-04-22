package com.batu.budgeting.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.batu.budgeting.entity.ProcessedTransaction;

public interface ProcessedTransactionRepository extends JpaRepository<ProcessedTransaction, UUID> {
}
