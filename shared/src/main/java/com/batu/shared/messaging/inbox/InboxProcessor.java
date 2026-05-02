package com.batu.shared.messaging.inbox;

import java.util.UUID;

import com.batu.shared.messaging.BaseEvent;

public class InboxProcessor {
    private final InboxEventStore inboxEventStore;

    public InboxProcessor(InboxEventStore inboxEventStore) {
        this.inboxEventStore = inboxEventStore;
    }

    public void process(BaseEvent<?> event, Runnable handler) {
        process(event.getEventId(), handler);
    }

    public void process(UUID eventId, Runnable handler) {
        if (inboxEventStore.exists(eventId)) {
            return;
        }

        handler.run();
        inboxEventStore.save(eventId);
    }
}
