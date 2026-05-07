package com.batu.plaid_adapter_service.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.batu.plaid_adapter_service.entity.Connection;

import jakarta.persistence.EntityManager;

@DataJpaTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import=",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.show_sql=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ConnectionRepositoryIT.PostgreSqlTestcontainersConfiguration.class)
class ConnectionRepositoryIT {

    private static final UUID USER_ID = UUID.fromString("82000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("82000000-0000-0000-0000-000000000002");

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void flywayMigration_whenRepositoryStarts_shouldCreateConnectionTableAndPersistEntity() {
        Connection connection = saveConnection(USER_ID, "item-1", "ins-1", "Test Bank", true);

        assertThat(connection.getConnectionId()).isNotNull();
        assertThat(connection.getProvider()).isEqualTo("PLAID");
        assertThat(connection.getCreatedAt()).isNotNull();
        assertThat(connection.getUpdatedAt()).isNotNull();
    }

    @Test
    void findByExternalId_whenConnectionExists_shouldReturnConnection() {
        Connection connection = saveConnection(USER_ID, "item-external", "ins-1", "Test Bank", true);

        var result = connectionRepository.findByExternalId("item-external");

        assertThat(result).isPresent();
        assertThat(result.get().getConnectionId()).isEqualTo(connection.getConnectionId());
    }

    @Test
    void findByExternalId_whenConnectionMissing_shouldReturnEmpty() {
        assertThat(connectionRepository.findByExternalId("missing-item")).isEmpty();
    }

    @Test
    void findByConnectionIdAndUserId_whenConnectionBelongsToUser_shouldReturnConnection() {
        Connection connection = saveConnection(USER_ID, "item-owned", "ins-1", "Test Bank", true);

        var result = connectionRepository.findByConnectionIdAndUserId(connection.getConnectionId(), USER_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getExternalId()).isEqualTo("item-owned");
    }

    @Test
    void findByConnectionIdAndUserId_whenConnectionIsForeign_shouldReturnEmpty() {
        Connection connection = saveConnection(OTHER_USER_ID, "item-foreign", "ins-1", "Test Bank", true);

        assertThat(connectionRepository.findByConnectionIdAndUserId(connection.getConnectionId(), USER_ID)).isEmpty();
    }

    @Test
    void findByUserIdAndActiveTrue_whenConnectionsExist_shouldReturnOnlyActiveConnectionsForUser() {
        Connection active = saveConnection(USER_ID, "item-active", "ins-1", "Active Bank", true);
        saveConnection(USER_ID, "item-inactive", "ins-1", "Inactive Bank", false);
        saveConnection(OTHER_USER_ID, "item-foreign", "ins-1", "Foreign Bank", true);

        List<Connection> result = connectionRepository.findByUserIdAndActiveTrue(USER_ID);

        assertThat(result).extracting(Connection::getConnectionId).containsExactly(active.getConnectionId());
    }

    @Test
    void findByUserIdAndInstitutionIdAndActiveTrue_whenConnectionsExist_shouldReturnOnlyActiveUserInstitutionMatches() {
        Connection matching = saveConnection(USER_ID, "item-match", "ins-match", "Match Bank", true);
        saveConnection(USER_ID, "item-other-institution", "ins-other", "Other Bank", true);
        saveConnection(USER_ID, "item-inactive", "ins-match", "Inactive Bank", false);
        saveConnection(OTHER_USER_ID, "item-foreign", "ins-match", "Foreign Bank", true);

        List<Connection> result = connectionRepository.findByUserIdAndInstitutionIdAndActiveTrue(USER_ID, "ins-match");

        assertThat(result).extracting(Connection::getConnectionId).containsExactly(matching.getConnectionId());
    }

    @Test
    void lockByConnectionId_whenConnectionExists_shouldReturnConnectionForUpdate() {
        Connection connection = saveConnection(USER_ID, "item-lock", "ins-1", "Lock Bank", true);

        var result = connectionRepository.lockByConnectionId(connection.getConnectionId());

        assertThat(result).isPresent();
        assertThat(result.get().getExternalId()).isEqualTo("item-lock");
    }

    @Test
    void lockByConnectionId_whenConnectionMissing_shouldReturnEmpty() {
        assertThat(connectionRepository.lockByConnectionId(UUID.fromString("82000000-0000-0000-0000-000000000099")))
                .isEmpty();
    }

    @Test
    void saveAndFlush_whenExternalIdIsDuplicated_shouldRejectUniqueConstraintViolation() {
        saveConnection(USER_ID, "item-duplicate", "ins-1", "First Bank", true);

        assertThatThrownBy(() -> saveConnection(OTHER_USER_ID, "item-duplicate", "ins-2", "Second Bank", true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlush_whenRequiredUserIdIsMissing_shouldRejectRequiredColumnViolation() {
        Connection connection = new Connection(null, "item-missing-user", "access-token", "ins-1", "Test Bank");

        assertThatThrownBy(() -> connectionRepository.saveAndFlush(connection))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlush_whenRequiredExternalIdIsMissing_shouldRejectRequiredColumnViolation() {
        Connection connection = new Connection(USER_ID, null, "access-token", "ins-1", "Test Bank");

        assertThatThrownBy(() -> connectionRepository.saveAndFlush(connection))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlush_whenRequiredAccessTokenIsMissing_shouldRejectRequiredColumnViolation() {
        Connection connection = new Connection(USER_ID, "item-missing-token", null, "ins-1", "Test Bank");

        assertThatThrownBy(() -> connectionRepository.saveAndFlush(connection))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlush_whenRequiredInstitutionIdIsMissing_shouldRejectRequiredColumnViolation() {
        Connection missingInstitutionId = new Connection(USER_ID, "item-missing-institution-id", "access-token", null,
                "Test Bank");
        assertThatThrownBy(() -> connectionRepository.saveAndFlush(missingInstitutionId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void saveAndFlush_whenRequiredInstitutionNameIsMissing_shouldRejectRequiredColumnViolation() {
        Connection missingInstitutionName = new Connection(USER_ID, "item-missing-institution-name", "access-token",
                "ins-1", null);
        assertThatThrownBy(() -> connectionRepository.saveAndFlush(missingInstitutionName))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Connection saveConnection(
            UUID userId,
            String externalId,
            String institutionId,
            String institutionName,
            boolean active) {
        Connection connection = new Connection(userId, externalId, "access-token-" + externalId, institutionId,
                institutionName);
        connection.setActive(active);
        Connection savedConnection = connectionRepository.saveAndFlush(connection);
        entityManager.clear();
        return savedConnection;
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
