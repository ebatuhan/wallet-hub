package com.batu.shared.messaging.event;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionRemoved {
    private UUID connectionId;
    private UUID userId;
    private String reason;
}
