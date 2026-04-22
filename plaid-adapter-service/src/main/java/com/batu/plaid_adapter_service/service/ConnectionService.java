package com.batu.plaid_adapter_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.batu.plaid_adapter_service.entity.Connection;

@Service
public interface ConnectionService {
    Connection readById(UUID connectionId);

    Connection create(Connection connection);

    void deleteById(UUID connectionId);

    Connection updateById(UUID connectionId, Connection source);

    Connection readByExternalId(String externalId);

    Connection readByIdAndUserId(UUID connectionId, UUID userId);

    List<Connection> readAll();

    List<Connection> readAllByUserId(UUID userId);

    Connection startSync(UUID connectionId);

    Connection completeSync(UUID connectionId, String cursor);

    Connection releaseSync(UUID connectionId);

    Connection markDisabled(UUID connectionId, String errorCode);

    Connection markRemoving(UUID connectionId, String errorCode);

    Connection markRemoved(UUID connectionId, String errorCode);
}
