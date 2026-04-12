package com.batu.plaid_adapter_service.service;

import java.time.Duration;
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

    List<Connection> readAll();

    boolean claimSync(UUID connectionId, Duration staleAfter);

    void completeSync(UUID connectionId, String cursor);

    void releaseSync(UUID connectionId);

    Connection markDisabled(UUID connectionId, String errorCode);

    Connection markRemoved(UUID connectionId, String errorCode);
}
