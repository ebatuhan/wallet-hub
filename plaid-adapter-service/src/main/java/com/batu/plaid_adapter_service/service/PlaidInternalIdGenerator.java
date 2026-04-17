package com.batu.plaid_adapter_service.service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batu.plaid_adapter_service.entity.Connection;

@Component
public class PlaidInternalIdGenerator {

    private static final String ACCOUNT_NAMESPACE = "wallet-hub:plaid:account";
    private static final String TRANSACTION_NAMESPACE = "wallet-hub:plaid:transaction";

    public UUID accountId(Connection connection, String plaidAccountId) {
        return stableId(ACCOUNT_NAMESPACE, connection, plaidAccountId);
    }

    public UUID transactionId(Connection connection, String plaidTransactionId) {
        return stableId(TRANSACTION_NAMESPACE, connection, plaidTransactionId);
    }

    private UUID stableId(String namespace, Connection connection, String providerResourceId) {
        String seed = namespace
                + ":" + connection.getUserId()
                + ":" + connection.getExternalId()
                + ":" + providerResourceId;
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }
}
