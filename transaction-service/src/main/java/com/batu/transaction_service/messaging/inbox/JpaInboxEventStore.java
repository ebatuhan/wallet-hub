package com.batu.transaction_service.messaging.inbox;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batu.shared.messaging.inbox.InboxEventStore;
import com.batu.transaction_service.entity.InboxEvent;
import com.batu.transaction_service.repository.InboxEventRepository;

@Component
public class JpaInboxEventStore implements InboxEventStore {
    private final InboxEventRepository inboxEventRepository;

    public JpaInboxEventStore(InboxEventRepository inboxEventRepository) {
        this.inboxEventRepository = inboxEventRepository;
    }

    @Override
    public boolean exists(UUID eventId) {
        return inboxEventRepository.existsById(eventId);
    }

    @Override
    public void save(UUID eventId) {
        inboxEventRepository.save(new InboxEvent(eventId));
    }
}
