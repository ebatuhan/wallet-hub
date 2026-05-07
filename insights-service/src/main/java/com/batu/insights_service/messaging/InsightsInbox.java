package com.batu.insights_service.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.batu.insights_service.entity.InboxEvent;
import com.batu.insights_service.repository.InboxEventRepository;
import com.batu.shared.messaging.BaseEvent;

@Component
public class InsightsInbox {
    private final InboxEventRepository inboxEventRepository;

    public InsightsInbox(InboxEventRepository inboxEventRepository) {
        this.inboxEventRepository = inboxEventRepository;
    }

    @Transactional(transactionManager = "inboxTransactionManager")
    public void process(BaseEvent<?> event, Runnable handler) {
        inboxEventRepository.lockByEventId(event.getEventId());

        if (inboxEventRepository.existsById(event.getEventId())) {
            return;
        }

        handler.run();
        inboxEventRepository.save(new InboxEvent(event.getEventId()));
    }
}
