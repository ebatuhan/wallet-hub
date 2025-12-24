package com.batu.plaid_adapter_service.service.impl;

import java.util.UUID;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.repository.ConnectionRepository;
import com.batu.plaid_adapter_service.service.ConnectionService;

@Service
public class ConnectionServiceImpl implements ConnectionService {

    private final ConnectionRepository connectionRepository;

    public ConnectionServiceImpl(ConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    @Override
    public Connection readById(UUID connectionId) {
        return connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("AAA"));

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
    public Connection updateById(UUID connectionId, Connection target) {
        Connection source = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new RuntimeException("H"));

        BeanUtils.copyProperties(target, source);

        return connectionRepository.save(source);
    }

}
