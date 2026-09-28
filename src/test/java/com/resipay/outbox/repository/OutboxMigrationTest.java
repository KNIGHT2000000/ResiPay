package com.resipay.outbox.repository;

import com.resipay.common.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutboxMigrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Flyway V3 migration creates outbox_events table with valid constraints")
    void shouldVerifyOutboxTableCreated() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'outbox_events'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Outbox table rejects invalid status values via check constraint")
    void shouldRejectInvalidOutboxStatus() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload, status, retry_count, created_at) " +
                        "VALUES (?, 'PAYMENT', '123', 'TEST', '{}'::jsonb, 'INVALID_STATUS', 0, NOW())",
                id
        )).hasMessageContaining("chk_outbox_status");
    }
}
