package com.batu.insights_service.messaging;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.batu.shared.messaging.BaseEvent;

@Component
public class InsightsInbox {
    private final JdbcTemplate jdbcTemplate;

    public InsightsInbox(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void process(BaseEvent<?> event, Runnable handler) {
        Integer count = jdbcTemplate.queryForObject(
                "select count() from clickhouse.inbox_events where event_id = ?",
                Integer.class,
                event.getEventId());
        if (count != null && count > 0) {
            return;
        }

        handler.run();
        jdbcTemplate.update("insert into clickhouse.inbox_events (event_id, processed_at) values (?, now64(9))",
                event.getEventId());
    }
}
