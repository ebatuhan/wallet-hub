package com.batu.plaid_adapter_service.service.impl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.repository.ConnectionRepository;
import com.batu.plaid_adapter_service.service.ConnectionService;

@ExtendWith(MockitoExtension.class)
public class ConnectionServiceImplTest {
    @Mock
    private ConnectionRepository connectionRepository;

    @InjectMocks
    private ConnectionServiceImpl connectionService;

    @Test
    void shouldCreateConnection_withRequiredFields(){


        Connection connection = new Connection(UUID.randomUUID(), UUID.randomUUID(), "external-id", "accessToken", "inst-id", "BankA");

        when(connectionRepository.save(any(Connection.class)))
        .thenAnswer(inv -> inv.getArgument(0));

        Connection result = connectionService.create(connection);

        assertEquals("external-id", result.getExternalId());

        verify(connectionRepository, times(1)).save(any(Connection.class));
    }


}
