package com.batu.insights_service.messaging.inbox;

import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.batu.shared.messaging.inbox.InboxEventStore;

@Component
public class JdbcInboxEventStore implements InboxEventStore {
    private final JdbcTemplate jdbcTemplate;

    public JdbcInboxEventStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean exists(UUID eventId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count() from clickhouse.inbox_events where event_id = ?",
                Integer.class,
                eventId);
        return count != null && count > 0;
    }

    @Override
    public void save(UUID eventId) {
        jdbcTemplate.update("insert into clickhouse.inbox_events (event_id, processed_at) values (?, now64(9))", eventId);
    }
}
