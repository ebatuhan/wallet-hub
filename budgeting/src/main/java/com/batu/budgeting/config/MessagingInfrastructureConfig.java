package com.batu.budgeting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.batu.shared.messaging.inbox.InboxEventStore;
import com.batu.shared.messaging.inbox.InboxProcessor;

@Configuration
public class MessagingInfrastructureConfig {
    @Bean
    InboxProcessor inboxProcessor(InboxEventStore inboxEventStore) {
        return new InboxProcessor(inboxEventStore);
    }
}
