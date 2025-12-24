package com.batu.plaid_adapter_service.service.impl;

import java.util.UUID;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.repository.ConnectionRepository;
import com.batu.plaid_adapter_service.service.ConnectionService;

public class ConnectionServiceImpl implements ConnectionService {

    private final ConnectionRepository connectionRepository;

    public ConnectionServiceImpl(ConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    @Override
    public Connection readById(UUID connectionId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'readById'");
    }

    @Override
    public Connection create(Connection connection) {
        return connectionRepository.save(connection);
    }

    @Override
    public void deleteById(UUID connectionId) {
        try{
            connectionRepository.deleteById(connectionId);
        }

        catch(IllegalArgumentException ex){
            //throw new BusinessLogicException("ID was null.");
        }
        
    }

    @Override
    public Connection updateById(UUID connectionId, Connection target) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateById'");
    }

}
