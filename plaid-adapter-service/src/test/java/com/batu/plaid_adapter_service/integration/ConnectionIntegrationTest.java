package com.batu.plaid_adapter_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.repository.ConnectionRepository;
import com.batu.plaid_adapter_service.service.ConnectionService;

import jakarta.transaction.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional

public class ConnectionIntegrationTest {

    private final ConnectionService connectionService;
    private final ConnectionRepository connectionRepository;

    private UUID sharedUUID;
    private Connection sharedConnection;

    @Autowired
    public ConnectionIntegrationTest(ConnectionService connectionService, ConnectionRepository connectionRepository) {
        this.connectionService = connectionService;
        this.connectionRepository = connectionRepository;
    }

    @BeforeEach
    void setUp() {
        Connection connection = new Connection(UUID.randomUUID(),
                "setup-external-id-test",
                "setup-access-token-test",
                "setup-ins01",
                "setup-Bank Test",
                "setup-ACTIVE",
                null,
                null);

        sharedConnection = connectionRepository.save(connection);

        sharedUUID = sharedConnection.getConnectionId();
    }

    @AfterEach
    void cleanUp() {
        connectionRepository.deleteAll();
    }

    @Test
    public void shouldCreateConnection() {
        Connection connection = new Connection(UUID.randomUUID(),
                "external-id-test",
                "access-token-test",
                "ins01",
                "Bank Test",
                "ACTIVE",
                null,
                null);

        Connection savedConnection = connectionService.create(connection);

        System.out.println(savedConnection.getConnectionId());

        assertNotNull(savedConnection);
        assertNotNull(savedConnection.getConnectionId());
    }

    @Test
    public void shouldReadCreatedConnection() {
        Connection readConnection = connectionService.readById(sharedUUID);
        assertEquals(sharedConnection, readConnection);
    }

    @Test
    public void shouldDeleteCreatedConnection() {
        connectionService.deleteById(sharedUUID);

        assertFalse(connectionRepository.existsById(sharedUUID));
    }

    @Test
    public void shouldUpdateConnection(){

        Connection connection = new Connection(UUID.randomUUID(),
            "changed-external-id-test",
            "changed-access-token-test",
            "changed-ins01",
            "changed-Bank Test",
            "changed-ACTIVE",
            null,
            null);

        connectionService.updateById(sharedUUID, sharedConnection);

        assertNotEquals(sharedConnection, connection);
    }
}
