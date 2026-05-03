package com.batu.budgeting.messaging;

import org.springframework.stereotype.Component;

import com.batu.budgeting.entity.InboxEvent;
import com.batu.budgeting.repository.InboxEventRepository;
import com.batu.shared.messaging.BaseEvent;

@Component
public class BudgetingInbox {
    private final InboxEventRepository inboxEventRepository;

    public BudgetingInbox(InboxEventRepository inboxEventRepository) {
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
