package com.batu.budgeting.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.batu.budgeting.entity.InboxEvent;
import com.batu.budgeting.repository.InboxEventRepository;
import com.batu.shared.messaging.BaseEvent;

@Component
public class BudgetingInbox {
    private final InboxEventRepository inboxEventRepository;

    public BudgetingInbox(InboxEventRepository inboxEventRepository) {
        this.inboxEventRepository = inboxEventRepository;
    }

    @Transactional
    public void process(BaseEvent<?> event, Runnable handler) {
        inboxEventRepository.lockByEventId(event.getEventId());

        if (inboxEventRepository.existsById(event.getEventId())) {
            return;
        }

        handler.run();
        inboxEventRepository.save(new InboxEvent(event.getEventId()));
    }
}
