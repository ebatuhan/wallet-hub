package com.batu.plaid_adapter_service.mapper;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.entity.Connection;

@Component
public class ConnectionMapper {

    public void updateConnectionFromSource(Connection source, Connection target) {
        if (source.getUserId() != null) {
            target.setUserId(source.getUserId());
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
        if (source.getConnectionStatus() != null) {
            target.setConnectionStatus(source.getConnectionStatus());
        }
        if (source.getErrorCode() != null) {
            target.setErrorCode(source.getErrorCode());
        }
        if (source.getLastCursor() != null) {
            target.setLastCursor(source.getLastCursor());
        }
    }
}
