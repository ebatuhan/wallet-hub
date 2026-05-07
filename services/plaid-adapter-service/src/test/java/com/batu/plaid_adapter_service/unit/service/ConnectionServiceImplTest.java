package com.batu.plaid_adapter_service.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.mapper.ConnectionMapper;
import com.batu.plaid_adapter_service.repository.ConnectionRepository;
import com.batu.plaid_adapter_service.service.impl.ConnectionServiceImpl;

@ExtendWith(MockitoExtension.class)
class ConnectionServiceImplTest {

    private static final UUID CONNECTION_ID = UUID.fromString("76000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString("76000000-0000-0000-0000-000000000002");

    @Mock
    private ConnectionRepository connectionRepository;

    private ConnectionServiceImpl connectionService;

    @BeforeEach
    void setUp() {
        connectionService = new ConnectionServiceImpl(connectionRepository, new ConnectionMapper());
    }

    @Test
    void readAll_whenConnectionsExist_shouldReturnRepositoryResults() {
        List<Connection> connections = List.of(connection(true));
        when(connectionRepository.findAll()).thenReturn(connections);

        assertThat(connectionService.readAll()).isSameAs(connections);
    }

    @Test
    void readAllByUserId_whenUserHasActiveConnections_shouldDelegateToActiveRepositoryQuery() {
        List<Connection> connections = List.of(connection(true));
        when(connectionRepository.findByUserIdAndActiveTrue(USER_ID)).thenReturn(connections);

        assertThat(connectionService.readAllByUserId(USER_ID)).isSameAs(connections);
    }

    @Test
    void readAllByUserIdAndInstitutionId_whenUserHasInstitutionConnections_shouldDelegateToRepository() {
        List<Connection> connections = List.of(connection(true));
        when(connectionRepository.findByUserIdAndInstitutionIdAndActiveTrue(USER_ID, "ins-1")).thenReturn(connections);

        assertThat(connectionService.readAllByUserIdAndInstitutionId(USER_ID, "ins-1")).isSameAs(connections);
    }

    @Test
    void completeSync_whenConnectionActive_shouldUpdateCursorClearErrorAndSave() {
        Connection connection = connection(true);
        connection.setErrorCode("OLD_ERROR");
        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.of(connection));
        when(connectionRepository.save(connection)).thenReturn(connection);

        Connection result = connectionService.completeSync(CONNECTION_ID, "cursor-2");

        assertThat(result).isSameAs(connection);
        assertThat(connection.getLastCursor()).isEqualTo("cursor-2");
        assertThat(connection.getErrorCode()).isNull();
        assertThat(connection.getLastSyncedAt()).isNotNull();
        verify(connectionRepository).save(connection);
    }

    @Test
    void completeSync_whenConnectionInactive_shouldReturnWithoutSaving() {
        Connection connection = connection(false);
        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.of(connection));

        Connection result = connectionService.completeSync(CONNECTION_ID, "cursor-2");

        assertThat(result).isSameAs(connection);
        assertThat(connection.getLastCursor()).isNull();
        verify(connectionRepository, never()).save(any());
    }

    @Test
    void completeSync_whenConnectionMissing_shouldThrowNotFound() {
        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectionService.completeSync(CONNECTION_ID, "cursor"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).contains("Connection with");
                });
    }

    @Test
    void readByIdForUpdate_whenConnectionMissing_shouldThrowNotFound() {
        when(connectionRepository.lockByConnectionId(CONNECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectionService.readByIdForUpdate(CONNECTION_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).contains("Connection with");
                });
    }

    @Test
    void readByExternalId_whenConnectionMissing_shouldThrowNotFound() {
        when(connectionRepository.findByExternalId("missing-item")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectionService.readByExternalId("missing-item"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).contains("Connection with");
                });
    }

    @Test
    void readByIdAndUserId_whenConnectionMissingOrForeign_shouldThrowNotFound() {
        when(connectionRepository.findByConnectionIdAndUserId(CONNECTION_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectionService.readByIdAndUserId(CONNECTION_ID, USER_ID))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).contains("Connection with");
                });
    }

    @Test
    void readByIdForUpdate_whenConnectionExists_shouldReturnLockedConnection() {
        Connection connection = connection(true);
        when(connectionRepository.lockByConnectionId(CONNECTION_ID)).thenReturn(Optional.of(connection));

        assertThat(connectionService.readByIdForUpdate(CONNECTION_ID)).isSameAs(connection);
    }

    @Test
    void create_whenConnectionProvided_shouldSaveConnection() {
        Connection connection = connection(true);
        when(connectionRepository.save(connection)).thenReturn(connection);

        assertThat(connectionService.create(connection)).isSameAs(connection);
        verify(connectionRepository).save(connection);
    }

    @Test
    void deleteById_whenConnectionIdProvided_shouldDelegateToRepository() {
        connectionService.deleteById(CONNECTION_ID);

        verify(connectionRepository).deleteById(CONNECTION_ID);
    }

    @Test
    void updateById_whenConnectionExists_shouldMapSourceAndSaveTarget() {
        Connection target = connection(true);
        Connection source = new Connection(USER_ID, "item-2", "new-token", "ins-2", "New Bank");
        source.setActive(false);
        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.of(target));
        when(connectionRepository.save(target)).thenReturn(target);

        Connection result = connectionService.updateById(CONNECTION_ID, source);

        assertThat(result).isSameAs(target);
        assertThat(target.getExternalId()).isEqualTo("item-2");
        assertThat(target.getInstitutionName()).isEqualTo("New Bank");
        assertThat(target.isActive()).isFalse();
        verify(connectionRepository).save(target);
    }

    @Test
    void updateById_whenConnectionMissing_shouldThrowNotFoundAndNotSave() {
        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> connectionService.updateById(CONNECTION_ID, connection(true)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).contains("Connection with");
                });
        verify(connectionRepository, never()).save(any());
    }

    @Test
    void readByExternalId_whenConnectionExists_shouldReturnConnection() {
        Connection connection = connection(true);
        when(connectionRepository.findByExternalId("item-1")).thenReturn(Optional.of(connection));

        assertThat(connectionService.readByExternalId("item-1")).isSameAs(connection);
    }

    @Test
    void readByIdAndUserId_whenConnectionBelongsToUser_shouldReturnConnection() {
        Connection connection = connection(true);
        when(connectionRepository.findByConnectionIdAndUserId(CONNECTION_ID, USER_ID)).thenReturn(Optional.of(connection));

        assertThat(connectionService.readByIdAndUserId(CONNECTION_ID, USER_ID)).isSameAs(connection);
    }

    @Test
    void deactivate_whenConnectionExists_shouldMarkInactiveSetErrorCodeAndSave() {
        Connection connection = connection(true);
        when(connectionRepository.findById(CONNECTION_ID)).thenReturn(Optional.of(connection));
        when(connectionRepository.save(connection)).thenReturn(connection);

        Connection result = connectionService.deactivate(CONNECTION_ID, "USER_PERMISSION_REVOKED");

        assertThat(result).isSameAs(connection);
        assertThat(connection.isActive()).isFalse();
        assertThat(connection.getErrorCode()).isEqualTo("USER_PERMISSION_REVOKED");
        verify(connectionRepository).save(connection);
    }

    private Connection connection(boolean active) {
        Connection connection = new Connection(USER_ID, "item-1", "access-token", "ins-1", "Test Bank");
        ReflectionTestUtils.setField(connection, "connectionId", CONNECTION_ID);
        connection.setActive(active);
        return connection;
    }
}
