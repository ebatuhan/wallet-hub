package com.batu.transaction_service.messaging;

import org.springframework.stereotype.Component;

import com.batu.shared.messaging.BaseEvent;
import com.batu.transaction_service.entity.InboxEvent;
import com.batu.transaction_service.repository.InboxEventRepository;

@Component
public class TransactionInbox {
    private final InboxEventRepository inboxEventRepository;

    public TransactionInbox(InboxEventRepository inboxEventRepository) {
        this.inboxEventRepository = inboxEventRepository;
    }

    public void process(BaseEvent<?> event, Runnable handler) {
        if (inboxEventRepository.existsById(event.getEventId())) {
            return;
        }

        handler.run();
        inboxEventRepository.save(new InboxEvent(event.getEventId()));
    }
}
