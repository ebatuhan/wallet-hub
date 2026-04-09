package com.batu.shared.messaging.saga;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class FetchTransactionsEvent {
    private UUID sagaId;
    
    private boolean isSuccesfull;
}
