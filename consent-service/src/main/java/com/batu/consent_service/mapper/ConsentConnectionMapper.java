package com.batu.consent_service.mapper;

import org.springframework.stereotype.Component;

import com.batu.consent_service.entity.ConsentConnection;
import com.batu.shared.dto.request.ConnectionUpdateRequestDto;
import com.batu.shared.dto.response.ConnectionResponseDto;

@Component
public class ConsentConnectionMapper {

    public ConnectionResponseDto toResponse(ConsentConnection connection) {
        String status = connection.getStatus();
        boolean supportsRefresh = "ACTIVE".equals(status)
                || "DISABLED".equals(status)
                || "FAILED".equals(status);
        boolean supportsReconnect = "DISABLED".equals(status)
                || "FAILED".equals(status);
        boolean supportsDisconnect = !"REMOVED".equals(status) && !"REMOVING".equals(status);

        return new ConnectionResponseDto(
                connection.getConnectionId(),
                connection.getProvider(),
                toProviderDisplayName(connection.getProvider()),
                connection.getInstitutionId(),
                connection.getInstitutionName(),
                connection.getDisplayName(),
                connection.getStatus(),
                connection.getLastSyncedAt(),
                supportsRefresh,
                supportsReconnect,
                supportsDisconnect);
    }

    public void updateFromRequest(ConnectionUpdateRequestDto request, ConsentConnection connection) {
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            connection.setDisplayName(request.getDisplayName().trim());
        }
    }

    public void updateFromProvider(ConnectionResponseDto providerResponse, ConsentConnection connection) {
        connection.setInstitutionId(providerResponse.getInstitutionId());
        connection.setInstitutionName(providerResponse.getInstitutionName());
        connection.setStatus(providerResponse.getStatus());
        connection.setLastSyncedAt(providerResponse.getLastSyncedAt());

        if (providerResponse.getDisplayName() != null && !providerResponse.getDisplayName().isBlank()) {
            connection.setDisplayName(providerResponse.getDisplayName());
        }
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
