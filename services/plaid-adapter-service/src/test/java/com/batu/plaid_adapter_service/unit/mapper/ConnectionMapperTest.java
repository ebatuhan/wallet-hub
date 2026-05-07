package com.batu.plaid_adapter_service.unit.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.mapper.ConnectionMapper;
import com.batu.shared.dto.request.ConnectionUpdateRequestDto;

class ConnectionMapperTest {

    private static final UUID USER_ID = UUID.fromString("73000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("73000000-0000-0000-0000-000000000002");

    private final ConnectionMapper mapper = new ConnectionMapper();

    @Test
    void updateConnectionFromSource_whenSourceHasNonNullFields_shouldCopyFieldsAndActiveState() {
        Connection target = connection(USER_ID, "item-1", "Old Bank");
        target.setDisplayName("Old Display");
        target.setActive(true);
        Connection source = connection(OTHER_USER_ID, "item-2", "New Bank");
        source.setProvider("OPEN_BANKING");
        source.setDisplayName("New Display");
        source.setActive(false);
        source.setErrorCode("ERROR");
        source.setLastCursor("cursor-2");
        Instant syncedAt = Instant.parse("2026-05-07T00:00:00Z");
        source.setLastSyncedAt(syncedAt);

        mapper.updateConnectionFromSource(source, target);

        assertThat(target.getUserId()).isEqualTo(OTHER_USER_ID);
        assertThat(target.getProvider()).isEqualTo("OPEN_BANKING");
        assertThat(target.getExternalId()).isEqualTo("item-2");
        assertThat(target.getAccessToken()).isEqualTo("token-item-2");
        assertThat(target.getInstitutionId()).isEqualTo("ins-item-2");
        assertThat(target.getInstitutionName()).isEqualTo("New Bank");
        assertThat(target.getDisplayName()).isEqualTo("New Display");
        assertThat(target.isActive()).isFalse();
        assertThat(target.getErrorCode()).isEqualTo("ERROR");
        assertThat(target.getLastCursor()).isEqualTo("cursor-2");
        assertThat(target.getLastSyncedAt()).isEqualTo(syncedAt);
    }

    @Test
    void updateConnectionFromSource_whenNullableFieldsAreNull_shouldKeepExistingValuesButCopyActiveState() {
        Connection target = connection(USER_ID, "item-1", "Old Bank");
        target.setDisplayName("Old Display");
        target.setErrorCode("OLD_ERROR");
        target.setLastCursor("old-cursor");
        Connection source = new Connection(null, null, null, null, null);
        source.setActive(false);

        mapper.updateConnectionFromSource(source, target);

        assertThat(target.getUserId()).isEqualTo(USER_ID);
        assertThat(target.getExternalId()).isEqualTo("item-1");
        assertThat(target.getDisplayName()).isEqualTo("Old Display");
        assertThat(target.getErrorCode()).isEqualTo("OLD_ERROR");
        assertThat(target.getLastCursor()).isEqualTo("old-cursor");
        assertThat(target.isActive()).isFalse();
    }

    @ParameterizedTest
    @MethodSource("displayNameCases")
    void updateConnectionFromRequest_whenDisplayNameVaries_shouldApplyOnlyNonBlankValues(
            String displayName,
            String expectedDisplayName) {
        Connection target = connection(USER_ID, "item-1", "Old Bank");
        target.setDisplayName("Original");

        mapper.updateConnectionFromRequest(new ConnectionUpdateRequestDto(displayName), target);

        assertThat(target.getDisplayName()).isEqualTo(expectedDisplayName);
    }

    @ParameterizedTest
    @MethodSource("providerDisplayCases")
    void toResponse_whenProviderVaries_shouldMapProviderDisplayName(String provider, String expectedDisplayName) {
        Connection connection = connection(USER_ID, "item-1", "Test Bank");
        connection.setProvider(provider);

        var response = mapper.toResponse(connection);

        assertThat(response.getProvider()).isEqualTo(provider);
        assertThat(response.getProviderDisplayName()).isEqualTo(expectedDisplayName);
        assertThat(response.getInstitutionName()).isEqualTo("Test Bank");
        assertThat(response.getDisplayName()).isEqualTo("Test Bank");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.isSupportsRefresh()).isTrue();
        assertThat(response.isSupportsReconnect()).isFalse();
        assertThat(response.isSupportsDisconnect()).isTrue();
    }

    @Test
    void toResponse_whenConnectionInactive_shouldExposeInactiveStatusAndNotDeletable() {
        Connection connection = connection(USER_ID, "item-1", "Test Bank");
        connection.setActive(false);

        var response = mapper.toResponse(connection);

        assertThat(response.getStatus()).isEqualTo("INACTIVE");
        assertThat(response.isSupportsRefresh()).isFalse();
        assertThat(response.isSupportsDisconnect()).isFalse();
    }

    private static Stream<Arguments> displayNameCases() {
        return Stream.of(
                Arguments.of("Updated", "Updated"),
                Arguments.of("  Updated  ", "Updated"),
                Arguments.of("", "Original"),
                Arguments.of("   ", "Original"),
                Arguments.of(null, "Original"));
    }

    private static Stream<Arguments> providerDisplayCases() {
        return Stream.of(
                Arguments.of("PLAID", "Plaid"),
                Arguments.of("YODLEE", "Yodlee"),
                Arguments.of("TINK", "Tink"),
                Arguments.of("OPEN_BANKING", "Open Banking"),
                Arguments.of("CUSTOM", "CUSTOM"));
    }

    private Connection connection(UUID userId, String externalId, String institutionName) {
        return new Connection(userId, externalId, "token-" + externalId, "ins-" + externalId, institutionName);
    }
}
