package com.batu.consent_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.batu.consent_service.entity.ConsentConnection;

public interface ConsentConnectionRepository extends JpaRepository<ConsentConnection, UUID> {

    List<ConsentConnection> findByUserId(UUID userId);

    Optional<ConsentConnection> findByConnectionIdAndUserId(UUID connectionId, UUID userId);

    Optional<ConsentConnection> findByProviderAndProviderConnectionId(String provider, UUID providerConnectionId);
}
