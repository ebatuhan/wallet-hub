package com.batu.plaid_adapter_service.unit.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.exception.ResourceNotFoundException;
import com.batu.plaid_adapter_service.repository.ConnectionRepository;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.impl.ConnectionServiceImpl;

@ExtendWith(MockitoExtension.class)
public class ConnectionServiceImplTest {
    @Mock
    private ConnectionRepository connectionRepository;

    @InjectMocks
    private ConnectionServiceImpl connectionService;

    @Test
    void sholdThrowResourceNotFoundException_whenReadById_thenThrowResourceNotFoundException() {

        UUID id = UUID.randomUUID();

        when(connectionRepository.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> connectionService.readById(id));
    }

    @Test
    void sholdThrowResourceNotFoundException_updateById_thenThrowResourceNotFoundException() {

        UUID id = UUID.randomUUID();
        Connection connection = mock(Connection.class);

        

        when(connectionRepository.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> connectionService.updateById(id, connection));
    }

}
