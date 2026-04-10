package com.batu.shared.messaging.saga;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InitialTransactionsFetchFailedEvent {
    private UUID sagaId;
    private UUID connectionId;
    private String errorCode;
    private String errorMessage;
}
