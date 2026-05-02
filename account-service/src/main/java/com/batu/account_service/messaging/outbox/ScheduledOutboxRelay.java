package com.batu.account_service.messaging.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.batu.shared.messaging.outbox.OutboxRelay;

@Component
public class ScheduledOutboxRelay {
    private final OutboxRelay outboxRelay;

    public ScheduledOutboxRelay(OutboxRelay outboxRelay) {
        this.outboxRelay = outboxRelay;
    }

    @Scheduled(fixedDelayString = "${wallet-hub.outbox.relay-delay-ms:1000}")
    @Transactional
    public void publishPending() {
        outboxRelay.publishPending();
    }
}
