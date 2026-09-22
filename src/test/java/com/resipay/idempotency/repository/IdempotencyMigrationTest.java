package com.resipay.idempotency.repository;

import com.resipay.common.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyMigrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Flyway V2 creates idempotency_records table successfully")
    void shouldVerifyIdempotencyRecordsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'idempotency_records'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Flyway V2 enforces unique idempotency key constraint")
    void shouldEnforceUniqueIdempotencyKeyConstraint() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        String sharedKey = "idem-key-" + UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO idempotency_records (id, idempotency_key, request_hash, request_path, request_method, status, created_at, updated_at, expires_at) " +
                        "VALUES (?, ?, 'hash123', '/api/v1/payments', 'POST', 'IN_PROGRESS', NOW(), NOW(), NOW() + interval '1 day')",
                id1, sharedKey
        );

        // Second insert with identical key must fail due to unique constraint
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO idempotency_records (id, idempotency_key, request_hash, request_path, request_method, status, created_at, updated_at, expires_at) " +
                        "VALUES (?, ?, 'hash456', '/api/v1/payments', 'POST', 'IN_PROGRESS', NOW(), NOW(), NOW() + interval '1 day')",
                id2, sharedKey
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Flyway V2 enforces status check constraint")
    void shouldEnforceStatusCheckConstraint() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO idempotency_records (id, idempotency_key, request_hash, request_path, request_method, status, created_at, updated_at, expires_at) " +
                        "VALUES (?, ?, 'hash123', '/api/v1/payments', 'POST', 'INVALID_STATUS', NOW(), NOW(), NOW() + interval '1 day')",
                id, "key-" + UUID.randomUUID()
        )).isInstanceOf(DataIntegrityViolationException.class);
    }
}
