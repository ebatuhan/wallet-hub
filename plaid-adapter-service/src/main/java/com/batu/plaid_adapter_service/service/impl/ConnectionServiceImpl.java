package com.batu.plaid_adapter_service.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.entity.enums.ConnectionStatus;
import com.batu.plaid_adapter_service.exception.ResourceNotFoundException;
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
        return connectionRepository.findByUserId(userId);
    }

    @Override
    public List<Connection> readAllByUserIdAndInstitutionId(UUID userId, String institutionId) {
        return connectionRepository.findByUserIdAndInstitutionId(userId, institutionId);
    }

    @Override
    @Transactional
    public Connection startSync(UUID connectionId) {
        Connection connection = readById(connectionId);
        if (ConnectionStatus.SYNCING.name().equals(connection.getConnectionStatus())
                || ConnectionStatus.DISABLED.name().equals(connection.getConnectionStatus())
                || ConnectionStatus.REMOVING.name().equals(connection.getConnectionStatus())
                || ConnectionStatus.REMOVED.name().equals(connection.getConnectionStatus())) {
            return null;
        }

        connection.setConnectionStatus(ConnectionStatus.SYNCING.name());
        connection.setErrorCode(null);
        return connectionRepository.save(connection);
    }

    @Override
    @Transactional
    public Connection completeSync(UUID connectionId, String cursor) {
        Connection connection = readById(connectionId);
        if (ConnectionStatus.REMOVED.name().equals(connection.getConnectionStatus())
                || ConnectionStatus.REMOVING.name().equals(connection.getConnectionStatus())
                || ConnectionStatus.DISABLED.name().equals(connection.getConnectionStatus())) {
            return connection;
        }

        connection.setLastCursor(cursor);
        connection.setLastSyncedAt(java.time.Instant.now());
        connection.setConnectionStatus(ConnectionStatus.ACTIVE.name());
        connection.setErrorCode(null);
        return connectionRepository.save(connection);
    }

    @Override
    @Transactional
    public Connection releaseSync(UUID connectionId) {
        Connection connection = readById(connectionId);
        if (ConnectionStatus.SYNCING.name().equals(connection.getConnectionStatus())) {
            connection.setConnectionStatus(ConnectionStatus.ACTIVE.name());
        }
        return connectionRepository.save(connection);
    }

    @Override
    public Connection readById(UUID connectionId) {
        return connectionRepository.findById(connectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection with " + connectionId + "not found"));
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
                .orElseThrow(() -> new ResourceNotFoundException("Connection with " + connectionId + "not found"));

        connectionMapper.updateConnectionFromSource(source, target);

        return connectionRepository.save(target);
    }

    @Override
    public Connection readByExternalId(String externalId) {
        return connectionRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Connection with " + externalId + "not found"));
    }

    @Override
    public Connection readByIdAndUserId(UUID connectionId, UUID userId) {
        return connectionRepository.findByConnectionIdAndUserId(connectionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Connection with " + connectionId + " not found for user " + userId));
    }

    @Override
    @Transactional
    public Connection markDisabled(UUID connectionId, String errorCode) {
        Connection connection = readById(connectionId);
        connection.setConnectionStatus(ConnectionStatus.DISABLED.name());
        connection.setErrorCode(errorCode);
        return connectionRepository.save(connection);
    }

    @Override
    @Transactional
    public Connection markRemoving(UUID connectionId, String errorCode) {
        Connection connection = readById(connectionId);
        if (ConnectionStatus.REMOVED.name().equals(connection.getConnectionStatus())) {
            return connection;
        }

        connection.setConnectionStatus(ConnectionStatus.REMOVING.name());
        connection.setErrorCode(errorCode);
        return connectionRepository.save(connection);
    }

    @Override
    @Transactional
    public Connection markRemoved(UUID connectionId, String errorCode) {
        Connection connection = readById(connectionId);
        connection.setConnectionStatus(ConnectionStatus.REMOVED.name());
        connection.setErrorCode(errorCode);
        return connectionRepository.save(connection);
    }

}
