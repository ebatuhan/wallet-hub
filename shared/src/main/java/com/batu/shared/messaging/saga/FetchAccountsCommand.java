package com.batu.shared.messaging.saga;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class FetchAccountsCommand {
    private UUID sagaId;
    private UUID connectionId;
}
