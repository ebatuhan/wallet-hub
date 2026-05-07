package com.batu.plaid_adapter_service.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.mapper.ConnectionMapper;
import com.batu.plaid_adapter_service.repository.ConnectionRepository;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.impl.ConnectionServiceImpl;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ ConnectionServiceImpl.class, ConnectionMapper.class, ConnectionServiceIT.PostgreSqlTestcontainersConfiguration.class })
class ConnectionServiceIT {

    private static final UUID USER_ID = UUID.fromString("83000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("83000000-0000-0000-0000-000000000002");

    @Autowired
    private ConnectionService connectionService;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createAndReadByUser_whenConnectionsExist_shouldPersistAndReturnOnlyActiveOwnedConnections() {
        Connection active = connectionService.create(connection(USER_ID, "item-active", true));
        connectionService.create(connection(USER_ID, "item-inactive", false));
        connectionService.create(connection(OTHER_USER_ID, "item-foreign", true));
        entityManager.flush();
        entityManager.clear();

        var result = connectionService.readAllByUserId(USER_ID);

        assertThat(result).extracting(Connection::getConnectionId).containsExactly(active.getConnectionId());
    }

    @Test
    void readAllByUserIdAndInstitutionId_whenConnectionsExist_shouldReturnOnlyActiveOwnedInstitutionConnections() {
        Connection matching = connectionService.create(connection(USER_ID, "item-match", true));
        connectionService.create(connection(USER_ID, "item-other-bank", true, "ins-other", "Other Bank"));
        connectionService.create(connection(USER_ID, "item-inactive", false));
        connectionService.create(connection(OTHER_USER_ID, "item-foreign", true));
        entityManager.flush();
        entityManager.clear();

        var result = connectionService.readAllByUserIdAndInstitutionId(USER_ID, "ins-1");

        assertThat(result).extracting(Connection::getConnectionId).containsExactly(matching.getConnectionId());
    }

    @Test
    void completeSync_whenConnectionActive_shouldPersistCursorTimestampAndClearError() {
        Connection connection = connectionService.create(connection(USER_ID, "item-sync", true));
        connection.setErrorCode("OLD_ERROR");
        connectionRepository.saveAndFlush(connection);
        entityManager.clear();

        Connection result = connectionService.completeSync(connection.getConnectionId(), "cursor-2");
        entityManager.flush();
        entityManager.clear();

        Connection persisted = connectionRepository.findById(connection.getConnectionId()).orElseThrow();
        assertThat(result.getConnectionId()).isEqualTo(connection.getConnectionId());
        assertThat(persisted.getLastCursor()).isEqualTo("cursor-2");
        assertThat(persisted.getErrorCode()).isNull();
        assertThat(persisted.getLastSyncedAt()).isNotNull();
    }

    @Test
    void completeSync_whenConnectionInactive_shouldLeaveCursorAndErrorUnchanged() {
        Connection connection = connectionService.create(connection(USER_ID, "item-inactive-sync", false));
        connection.setLastCursor("old-cursor");
        connection.setErrorCode("ITEM_LOGIN_REQUIRED");
        connectionRepository.saveAndFlush(connection);
        entityManager.clear();

        connectionService.completeSync(connection.getConnectionId(), "new-cursor");
        entityManager.flush();
        entityManager.clear();

        Connection persisted = connectionRepository.findById(connection.getConnectionId()).orElseThrow();
        assertThat(persisted.getLastCursor()).isEqualTo("old-cursor");
        assertThat(persisted.getErrorCode()).isEqualTo("ITEM_LOGIN_REQUIRED");
    }

    @Test
    void updateById_whenConnectionExists_shouldPersistMappedFields() {
        Connection target = connectionService.create(connection(USER_ID, "item-target", true));
        Connection source = connection(USER_ID, "item-source", false, "ins-2", "Updated Bank");
        source.setDisplayName("Updated Display");
        source.setLastCursor("cursor-3");

        connectionService.updateById(target.getConnectionId(), source);
        entityManager.flush();
        entityManager.clear();

        Connection persisted = connectionRepository.findById(target.getConnectionId()).orElseThrow();
        assertThat(persisted.getExternalId()).isEqualTo("item-source");
        assertThat(persisted.getInstitutionName()).isEqualTo("Updated Bank");
        assertThat(persisted.getDisplayName()).isEqualTo("Updated Display");
        assertThat(persisted.getLastCursor()).isEqualTo("cursor-3");
        assertThat(persisted.isActive()).isFalse();
    }

    @Test
    void deactivate_whenConnectionExists_shouldPersistInactiveStateAndReason() {
        Connection connection = connectionService.create(connection(USER_ID, "item-deactivate", true));
        entityManager.flush();
        entityManager.clear();

        connectionService.deactivate(connection.getConnectionId(), "USER_PERMISSION_REVOKED");
        entityManager.flush();
        entityManager.clear();

        Connection persisted = connectionRepository.findById(connection.getConnectionId()).orElseThrow();
        assertThat(persisted.isActive()).isFalse();
        assertThat(persisted.getErrorCode()).isEqualTo("USER_PERMISSION_REVOKED");
    }

    @Test
    void readByIdAndUserId_whenConnectionIsForeign_shouldThrowNotFound() {
        Connection connection = connectionService.create(connection(OTHER_USER_ID, "item-foreign", true));
        entityManager.flush();
        entityManager.clear();

        assertThatThrownBy(() -> connectionService.readByIdAndUserId(connection.getConnectionId(), USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void readByIdForUpdate_whenConnectionExists_shouldReturnLockedConnection() {
        Connection connection = connectionService.create(connection(USER_ID, "item-lock", true));
        entityManager.flush();
        entityManager.clear();

        Connection result = connectionService.readByIdForUpdate(connection.getConnectionId());

        assertThat(result.getExternalId()).isEqualTo("item-lock");
    }

    private Connection connection(UUID userId, String externalId, boolean active) {
        return connection(userId, externalId, active, "ins-1", "Test Bank");
    }

    private Connection connection(UUID userId, String externalId, boolean active, String institutionId, String institutionName) {
        Connection connection = new Connection(userId, externalId, "access-token-" + externalId, institutionId,
                institutionName);
        connection.setActive(active);
        return connection;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PostgreSqlTestcontainersConfiguration {

        @Bean
        @ServiceConnection
        PostgreSQLContainer postgresContainer() {
            return new PostgreSQLContainer("postgres:16-alpine");
        }
    }
}
