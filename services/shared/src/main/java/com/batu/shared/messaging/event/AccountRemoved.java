package com.batu.shared.messaging.event;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AccountRemoved {
    private UUID accountId;
    private UUID userId;
    private UUID connectionId;
}
