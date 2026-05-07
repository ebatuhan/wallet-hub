package com.batu.account_service.unit.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.batu.account_service.entity.Account;
import com.batu.account_service.entity.InboxEvent;
import com.batu.account_service.entity.OutboxEvent;
import com.batu.shared.messaging.EventTypes;
import com.batu.shared.messaging.MessagingTopology;

class AccountTest {

    @Test
    void isNew_whenAccountHasNotBeenPersisted_shouldReturnTrue() {
        Account account = account();

        assertThat(account.isNew()).isTrue();
    }

    @Test
    void isNew_whenAccountHasCreatedAtTimestamp_shouldReturnFalse() {
        Account account = account();
        ReflectionTestUtils.setField(account, "createdAt", Instant.parse("2026-05-06T10:15:30Z"));

        assertThat(account.isNew()).isFalse();
    }

    @Test
    void getId_whenAccountIdWasProvided_shouldReturnAccountId() {
        UUID accountId = UUID.fromString("a0000000-0000-0000-0000-000000000001");
        Account account = new Account(
                accountId,
                UUID.fromString("a0000000-0000-0000-0000-000000000002"),
                UUID.fromString("a0000000-0000-0000-0000-000000000003"),
                "Bank",
                "Checking",
                "depository",
                "checking",
                "1234",
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                "USD",
                true);

        assertThat(account.getId()).isEqualTo(accountId);
    }

    @Test
    void inboxEvent_whenCreated_shouldSetEventIdAndProcessedAt() {
        UUID eventId = UUID.fromString("a0000000-0000-0000-0000-000000000004");

        InboxEvent inboxEvent = new InboxEvent(eventId);

        assertThat(inboxEvent.getEventId()).isEqualTo(eventId);
        assertThat(inboxEvent.getProcessedAt()).isNotNull();
    }

    @Test
    void outboxEvent_whenCreated_shouldExposeRoutingMetadataAndPayload() {
        UUID accountId = UUID.fromString("a0000000-0000-0000-0000-000000000005");

        OutboxEvent outboxEvent = new OutboxEvent(
                MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY,
                EventTypes.ACCOUNT_RECORDED,
                "account",
                accountId,
                "{\"eventType\":\"account.recorded.v1\"}");

        assertThat(outboxEvent.getRoutingKey()).isEqualTo(MessagingTopology.ACCOUNT_RECORDED_ROUTING_KEY);
        assertThat(outboxEvent.getEventType()).isEqualTo(EventTypes.ACCOUNT_RECORDED);
        assertThat(outboxEvent.getAggregateType()).isEqualTo("account");
        assertThat(outboxEvent.getAggregateId()).isEqualTo(accountId);
        assertThat(outboxEvent.getPayload()).contains("account.recorded.v1");
        assertThat(outboxEvent.getPublishedAt()).isNull();
    }

    @Test
    void markPublished_whenCalled_shouldSetPublishedAt() {
        OutboxEvent outboxEvent = new OutboxEvent(
                MessagingTopology.ACCOUNT_REMOVED_ROUTING_KEY,
                EventTypes.ACCOUNT_REMOVED,
                "account",
                UUID.fromString("a0000000-0000-0000-0000-000000000006"),
                "{\"eventType\":\"account.removed.v1\"}");

        outboxEvent.markPublished();

        assertThat(outboxEvent.getPublishedAt()).isNotNull();
    }

    private Account account() {
        return new Account(
                UUID.fromString("a0000000-0000-0000-0000-000000000002"),
                UUID.fromString("a0000000-0000-0000-0000-000000000003"),
                "Bank",
                "Checking",
                "depository",
                "checking",
                "1234",
                new BigDecimal("100.00"),
                new BigDecimal("90.00"),
                "USD",
                true);
    }
}
