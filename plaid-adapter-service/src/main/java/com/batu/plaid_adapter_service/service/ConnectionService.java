package com.batu.plaid_adapter_service.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.batu.plaid_adapter_service.entity.Connection;

@Service
public interface ConnectionService {
    Connection readById(UUID connectionId);

    Connection create(Connection connection);

    void deleteById(UUID connectionId);

    Connection updateById(UUID connectionId, Connection target);
}
