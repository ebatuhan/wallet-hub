package com.batu.shared.messaging.event;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRemoved {
    private UUID transactionId;
    private UUID userId;
    private UUID accountId;
}
