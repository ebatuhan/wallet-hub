package com.batu.shared.messaging.inbox;

import java.util.UUID;

public interface InboxEventStore {
    boolean exists(UUID eventId);

    void save(UUID eventId);
}
