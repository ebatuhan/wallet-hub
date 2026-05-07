package com.batu.plaid_adapter_service.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class DeterministicIdGenerator {

    public UUID accountId(UUID userId, String externalAccountId) {
        return deterministicUuid("plaid-account", userId.toString(), externalAccountId);
    }

    public UUID transactionId(UUID userId, String externalAccountId, String externalTransactionId) {
        return deterministicUuid("plaid-transaction", userId.toString(), externalAccountId, externalTransactionId);
    }

    private UUID deterministicUuid(String namespace, String... parts) {
        return UUID.nameUUIDFromBytes(String.join("|", prepend(namespace, parts)).getBytes(StandardCharsets.UTF_8));
    }

    private String[] prepend(String namespace, String[] parts) {
        String[] values = new String[parts.length + 1];
        values[0] = namespace;
        System.arraycopy(parts, 0, values, 1, parts.length);
        return values;
    }
}
