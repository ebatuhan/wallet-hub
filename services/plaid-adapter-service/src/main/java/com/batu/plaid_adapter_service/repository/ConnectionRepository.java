package com.batu.plaid_adapter_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.batu.plaid_adapter_service.entity.Connection;

import jakarta.persistence.LockModeType;

@Repository

public interface ConnectionRepository extends JpaRepository<Connection, UUID> {

    Optional<Connection> findByExternalId(String externalId);

    Optional<Connection> findByConnectionIdAndUserId(UUID connectionId, UUID userId);

    List<Connection> findByUserIdAndActiveTrue(UUID userId);

    List<Connection> findByUserIdAndInstitutionIdAndActiveTrue(UUID userId, String institutionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select connection
            from Connection connection
            where connection.connectionId = :connectionId
            """)
    Optional<Connection> lockByConnectionId(@Param("connectionId") UUID connectionId);
}
