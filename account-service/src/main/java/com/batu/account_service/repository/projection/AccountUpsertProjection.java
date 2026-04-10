package com.batu.account_service.repository.projection;

import java.util.UUID;

public interface AccountUpsertProjection {
    UUID getAccountId();
    String getExternalId();
}