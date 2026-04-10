package com.batu.shared.messaging.saga;

import java.util.Map;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FetchInitialTransactionsCommand {
    private UUID sagaId;
    private UUID connectionId;
    private Map<String, UUID> accountIdMap;
}
