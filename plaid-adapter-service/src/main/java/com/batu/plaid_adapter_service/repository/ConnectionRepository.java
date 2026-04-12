package com.batu.plaid_adapter_service.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.batu.plaid_adapter_service.entity.Connection;

@Repository

public interface ConnectionRepository extends JpaRepository<Connection, UUID> {

    Optional<Connection> findByExternalId(String externalId);

    @Modifying
    @Query("""
            update Connection connection
            set connection.connectionStatus = :syncingStatus,
                connection.updatedAt = CURRENT_TIMESTAMP
            where connection.connectionId = :connectionId
              and connection.connectionStatus not in ('DISABLED', 'REMOVED')
              and (connection.connectionStatus <> :syncingStatus or connection.updatedAt < :staleBefore)
            """)
    int claimSync(
            @Param("connectionId") UUID connectionId,
            @Param("syncingStatus") String syncingStatus,
            @Param("staleBefore") Instant staleBefore);

    @Modifying
    @Query("""
            update Connection connection
            set connection.connectionStatus = :activeStatus,
                connection.lastCursor = :cursor,
                connection.updatedAt = CURRENT_TIMESTAMP
            where connection.connectionId = :connectionId
            """)
    int completeSync(
            @Param("connectionId") UUID connectionId,
            @Param("activeStatus") String activeStatus,
            @Param("cursor") String cursor);

    @Modifying
    @Query("""
            update Connection connection
            set connection.connectionStatus = :activeStatus,
                connection.updatedAt = CURRENT_TIMESTAMP
            where connection.connectionId = :connectionId
            """)
    int releaseSync(
            @Param("connectionId") UUID connectionId,
            @Param("activeStatus") String activeStatus);
    
}
