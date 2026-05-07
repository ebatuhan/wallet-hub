package com.batu.plaid_adapter_service.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.mapper.ConnectionMapper;
import com.batu.plaid_adapter_service.repository.ConnectionRepository;
import com.batu.plaid_adapter_service.service.ConnectionService;

import jakarta.transaction.Transactional;

@Service
public class ConnectionServiceImpl implements ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final ConnectionMapper connectionMapper;

    public ConnectionServiceImpl(ConnectionRepository connectionRepository,
            ConnectionMapper connectionMapper) {
        this.connectionRepository = connectionRepository;
        this.connectionMapper = connectionMapper;
    }

    @Override
    public List<Connection> readAll() {
        return connectionRepository.findAll();
    }

    @Override
    public List<Connection> readAllByUserId(UUID userId) {
        return connectionRepository.findByUserIdAndActiveTrue(userId);
    }

    @Override
    public List<Connection> readAllByUserIdAndInstitutionId(UUID userId, String institutionId) {
        return connectionRepository.findByUserIdAndInstitutionIdAndActiveTrue(userId, institutionId);
    }

    @Override
    @Transactional
    public Connection completeSync(UUID connectionId, String cursor) {
        Connection connection = readById(connectionId);
        if (!connection.isActive()) {
            return connection;
        }

        connection.setLastCursor(cursor);
        connection.setLastSyncedAt(java.time.Instant.now());
        connection.setErrorCode(null);
        return connectionRepository.save(connection);
    }

    private Connection readById(UUID connectionId) {
        return connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Connection with " + connectionId + "not found"));
    }

    @Override
    @Transactional
    public Connection readByIdForUpdate(UUID connectionId) {
        return connectionRepository.lockByConnectionId(connectionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Connection with " + connectionId + "not found"));
    }

    @Override
    public Connection create(Connection connection) {
        return connectionRepository.save(connection);
    }

    @Override
    public void deleteById(UUID connectionId) {
        connectionRepository.deleteById(connectionId);
    }

    @Override
    public Connection updateById(UUID connectionId, Connection source) {
        Connection target = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Connection with " + connectionId + "not found"));

        connectionMapper.updateConnectionFromSource(source, target);

        return connectionRepository.save(target);
    }

    @Override
    public Connection readByExternalId(String externalId) {
        return connectionRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Connection with " + externalId + "not found"));
    }

    @Override
    public Connection readByIdAndUserId(UUID connectionId, UUID userId) {
        return connectionRepository.findByConnectionIdAndUserId(connectionId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Connection with " + connectionId + " not found for user " + userId));
    }

    @Override
    @Transactional
    public Connection deactivate(UUID connectionId, String errorCode) {
        Connection connection = readById(connectionId);
        connection.setActive(false);
        connection.setErrorCode(errorCode);
        return connectionRepository.save(connection);
    }


}
