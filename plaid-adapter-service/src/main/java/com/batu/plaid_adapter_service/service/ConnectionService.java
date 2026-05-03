package com.batu.plaid_adapter_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.batu.plaid_adapter_service.entity.Connection;

@Service
public interface ConnectionService {
    Connection create(Connection connection);

    void deleteById(UUID connectionId);

    Connection updateById(UUID connectionId, Connection source);

    Connection readByExternalId(String externalId);

    Connection readByIdAndUserId(UUID connectionId, UUID userId);

    Connection readByIdForUpdate(UUID connectionId);

    List<Connection> readAll();

    List<Connection> readAllByUserId(UUID userId);

    List<Connection> readAllByUserIdAndInstitutionId(UUID userId, String institutionId);

    Connection completeSync(UUID connectionId, String cursor);

    Connection deactivate(UUID connectionId, String errorCode);
}
