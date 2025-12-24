package com.batu.plaid_adapter_service.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.batu.plaid_adapter_service.entity.Connection;

@Repository

public interface ConnectionRepository extends JpaRepository<Connection, UUID> {
    
}
