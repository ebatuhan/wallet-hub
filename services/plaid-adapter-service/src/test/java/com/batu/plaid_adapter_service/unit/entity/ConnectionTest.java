package com.batu.plaid_adapter_service.unit.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.batu.plaid_adapter_service.entity.Connection;

class ConnectionTest {

    private static final UUID USER_ID = UUID.fromString("71000000-0000-0000-0000-000000000001");

    @Test
    void constructor_whenMinimalFieldsProvided_shouldDefaultProviderActiveAndDisplayName() {
        Connection connection = new Connection(USER_ID, "item-1", "access-token", "ins-1", "Test Bank");

        assertThat(connection.getUserId()).isEqualTo(USER_ID);
        assertThat(connection.getProvider()).isEqualTo("PLAID");
        assertThat(connection.getExternalId()).isEqualTo("item-1");
        assertThat(connection.getAccessToken()).isEqualTo("access-token");
        assertThat(connection.getInstitutionId()).isEqualTo("ins-1");
        assertThat(connection.getInstitutionName()).isEqualTo("Test Bank");
        assertThat(connection.getDisplayName()).isEqualTo("Test Bank");
        assertThat(connection.isActive()).isTrue();
        assertThat(connection.getErrorCode()).isNull();
        assertThat(connection.getLastCursor()).isNull();
    }

    @Test
    void constructor_whenStateFieldsProvided_shouldApplyConnectionState() {
        Connection connection = new Connection(USER_ID, "item-1", "access-token", "ins-1", "Test Bank", false,
                "ITEM_LOGIN_REQUIRED", "cursor-1");

        assertThat(connection.isActive()).isFalse();
        assertThat(connection.getErrorCode()).isEqualTo("ITEM_LOGIN_REQUIRED");
        assertThat(connection.getLastCursor()).isEqualTo("cursor-1");
    }
}
