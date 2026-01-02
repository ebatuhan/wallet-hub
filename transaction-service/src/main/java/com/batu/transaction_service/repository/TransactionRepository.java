package com.batu.transaction_service.repository;

import com.batu.transaction_service.entity.Transaction;

import org.springframework.data.domain.Window;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TransactionRepository extends 
    JpaRepository<Transaction, UUID>, 
    JpaSpecificationExecutor<Transaction> {
    
}