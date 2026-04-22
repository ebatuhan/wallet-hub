package com.batu.plaid_adapter_service.mapper;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.entity.enums.ConnectionStatus;
import com.batu.shared.dto.request.ConnectionUpdateRequestDto;
import com.batu.shared.dto.response.ConnectionResponseDto;

@Component
public class ConnectionMapper {

    public void updateConnectionFromSource(Connection source, Connection target) {
        if (source.getUserId() != null) {
            target.setUserId(source.getUserId());
        }
        if (source.getProvider() != null) {
            target.setProvider(source.getProvider());
        }
        if (source.getExternalId() != null) {
            target.setExternalId(source.getExternalId());
        }
        if (source.getAccessToken() != null) {
            target.setAccessToken(source.getAccessToken());
        }
        if (source.getInstitutionId() != null) {
            target.setInstitutionId(source.getInstitutionId());
        }
        if (source.getInstitutionName() != null) {
            target.setInstitutionName(source.getInstitutionName());
        }
        if (source.getDisplayName() != null) {
            target.setDisplayName(source.getDisplayName());
        }
        if (source.getConnectionStatus() != null) {
            target.setConnectionStatus(source.getConnectionStatus());
        }
        if (source.getErrorCode() != null) {
            target.setErrorCode(source.getErrorCode());
        }
        if (source.getLastCursor() != null) {
            target.setLastCursor(source.getLastCursor());
        }
        if (source.getLastSyncedAt() != null) {
            target.setLastSyncedAt(source.getLastSyncedAt());
        }
    }

    public void updateConnectionFromRequest(ConnectionUpdateRequestDto source, Connection target) {
        if (source.getDisplayName() != null && !source.getDisplayName().isBlank()) {
            target.setDisplayName(source.getDisplayName().trim());
        }
    }

    public ConnectionResponseDto toResponse(Connection connection) {
        String status = connection.getConnectionStatus();
        boolean supportsRefresh = ConnectionStatus.ACTIVE.name().equals(status)
                || ConnectionStatus.DISABLED.name().equals(status)
                || ConnectionStatus.FAILED.name().equals(status);
        boolean supportsReconnect = ConnectionStatus.DISABLED.name().equals(status)
                || ConnectionStatus.FAILED.name().equals(status);
        boolean supportsDisconnect = !ConnectionStatus.REMOVED.name().equals(status)
                && !ConnectionStatus.REMOVING.name().equals(status);

        return new ConnectionResponseDto(
                connection.getConnectionId(),
                connection.getProvider(),
                toProviderDisplayName(connection.getProvider()),
                connection.getInstitutionId(),
                connection.getInstitutionName(),
                connection.getDisplayName(),
                status,
                connection.getLastSyncedAt(),
                supportsRefresh,
                supportsReconnect,
                supportsDisconnect);
    }

    private String toProviderDisplayName(String provider) {
        return switch (provider) {
            case "PLAID" -> "Plaid";
            case "YODLEE" -> "Yodlee";
            case "TINK" -> "Tink";
            case "OPEN_BANKING" -> "Open Banking";
            default -> provider;
        };
    }
}
