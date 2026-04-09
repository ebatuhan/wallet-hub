package com.batu.shared.messaging.saga;

import java.util.HashMap;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class PersistAccountsEvent {
    private UUID sagaId;
    private boolean isSuccesfull;
    private HashMap<String, UUID> accountIdMap;
}
