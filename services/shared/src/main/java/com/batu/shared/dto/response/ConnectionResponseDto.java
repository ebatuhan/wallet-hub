package com.batu.shared.dto.response;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionResponseDto {

    private UUID connectionId;
    private String provider;
    private String providerDisplayName;
    private String institutionId;
    private String institutionName;
    private String displayName;
    private String status;
    private Instant lastSyncedAt;
    private boolean supportsRefresh;
    private boolean supportsReconnect;
    private boolean supportsDisconnect;
}
