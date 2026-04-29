package com.batu.plaid_adapter_service.service.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.batu.plaid_adapter_service.entity.AccountRegistry;
import com.batu.plaid_adapter_service.entity.Connection;
import com.batu.plaid_adapter_service.entity.enums.ConnectionStatus;
import com.batu.plaid_adapter_service.exception.DuplicateConnectionException;
import com.batu.plaid_adapter_service.service.ConnectionService;
import com.batu.plaid_adapter_service.service.RegistryService;
import com.batu.plaid_adapter_service.util.FingerprintHasher;
import com.batu.shared.dto.request.ConnectionAccountMetadataDto;
import com.batu.shared.dto.request.ExchangeTokenRequestDto;

@Service
public class PlaidDuplicateConnectionService {

    private final ConnectionService connectionService;
    private final RegistryService registryService;
    private final FingerprintHasher fingerprintHasher;

    public PlaidDuplicateConnectionService(ConnectionService connectionService,
            RegistryService registryService,
            FingerprintHasher fingerprintHasher) {
        this.connectionService = connectionService;
        this.registryService = registryService;
        this.fingerprintHasher = fingerprintHasher;
    }

    public void validateNotDuplicate(ExchangeTokenRequestDto request, UUID userId) {
        if (request.getAccounts() == null || request.getAccounts().isEmpty()) {
            return;
        }

        Set<String> incomingFingerprints = new HashSet<>();
        for (ConnectionAccountMetadataDto account : request.getAccounts()) {
            String fingerprint = createFingerprint(account.getName(), account.getMask());
            if (!fingerprint.isBlank()) {
                incomingFingerprints.add(fingerprint);
            }
        }

        if (incomingFingerprints.isEmpty()) {
            return;
        }

        for (Connection connection : connectionService.readAllByUserIdAndInstitutionId(userId, request.getInstitutionId())) {
            String status = connection.getConnectionStatus();
            if (ConnectionStatus.REMOVING.name().equals(status) || ConnectionStatus.REMOVED.name().equals(status)) {
                continue;
            }

            for (AccountRegistry accountRegistry : registryService.findAccountsByConnection(connection.getConnectionId())) {
                if (accountRegistry.getFingerprint() != null
                        && incomingFingerprints.contains(accountRegistry.getFingerprint())) {
                    throw new DuplicateConnectionException("This account is already connected.");
                }
            }
        }
    }

    public String createFingerprint(String accountName, String mask) {
        String normalizedName = normalizeFingerprintPart(accountName);
        String normalizedMask = normalizeFingerprintPart(mask);

        if (normalizedName.isBlank() || normalizedMask.isBlank()) {
            return "";
        }

        String source = normalizedName + "|" + normalizedMask;
        return fingerprintHasher.sha256(source);
    }

    private String normalizeFingerprintPart(String value) {
        return value == null ? "" : value.trim().toLowerCase().replaceAll("\\s+", " ");
    }
}
