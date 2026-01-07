package com.batu.plaid_adapter_service.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.exception.ResourceNotFoundException;
import com.batu.plaid_adapter_service.mapper.ConnectionMapper;
import com.batu.plaid_adapter_service.repository.ConnectionRepository;
import com.batu.plaid_adapter_service.service.ConnectionService;

@Service
public class ConnectionServiceImpl implements ConnectionService {

    private final ConnectionRepository connectionRepository;
    private final ConnectionMapper connectionMapper;

    public ConnectionServiceImpl(ConnectionRepository connectionRepository, ConnectionMapper connectionMapper) {
        this.connectionRepository = connectionRepository;
        this.connectionMapper = connectionMapper;
    }

    @Override
    public List<Connection> readAll() {
        return connectionRepository.findAll();
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

}
