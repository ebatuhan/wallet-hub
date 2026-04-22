package com.batu.plaid_adapter_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;

import com.batu.plaid_adapter_service.TestSupportConfiguration;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.entity.enums.ConnectionStatus;
import com.batu.plaid_adapter_service.repository.ConnectionRepository;
import com.batu.plaid_adapter_service.service.ConnectionService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestSupportConfiguration.class)
class ConnectionServiceConcurrencyIntegrationTest {

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ConnectionRepository connectionRepository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        connectionRepository.deleteAll();
    }

    @Test
    void startSync_allowsOnlyOneConcurrentWinner() throws Exception {
        Connection connection = connectionRepository.save(
                new Connection(UUID.randomUUID(), "item-1", "access-token", "ins-1", "Test Bank"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Future<Connection> first = executor.submit(() -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return connectionService.startSync(connection.getConnectionId());
        });
        Future<Connection> second = executor.submit(() -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return connectionService.startSync(connection.getConnectionId());
        });

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        Connection firstResult = first.get(5, TimeUnit.SECONDS);
        Connection secondResult = second.get(5, TimeUnit.SECONDS);
        executor.shutdownNow();

        long winnerCount = java.util.stream.Stream.of(firstResult, secondResult)
                .filter(Objects::nonNull)
                .count();

        assertEquals(1, winnerCount);

        Connection persistedConnection = connectionService.readById(connection.getConnectionId());
        assertNotNull(persistedConnection);
        assertEquals(ConnectionStatus.SYNCING.name(), persistedConnection.getConnectionStatus());
    }
}
