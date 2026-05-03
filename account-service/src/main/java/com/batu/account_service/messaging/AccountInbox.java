package com.batu.account_service.messaging;

import org.springframework.stereotype.Component;

import com.batu.account_service.entity.InboxEvent;
import com.batu.account_service.repository.InboxEventRepository;
import com.batu.shared.messaging.BaseEvent;

@Component
public class AccountInbox {
    private final InboxEventRepository inboxEventRepository;

    public AccountInbox(InboxEventRepository inboxEventRepository) {
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
