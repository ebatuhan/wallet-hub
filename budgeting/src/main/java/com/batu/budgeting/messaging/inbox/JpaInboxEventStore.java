package com.batu.budgeting.messaging.inbox;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.batu.budgeting.entity.InboxEvent;
import com.batu.budgeting.repository.InboxEventRepository;
import com.batu.shared.messaging.inbox.InboxEventStore;

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
