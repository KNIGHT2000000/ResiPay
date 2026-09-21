package com.resipay.payment.repository;

import com.resipay.common.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlywayMigrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Flyway creates payments table and enforces positive amount constraint")
    void shouldEnforcePositiveAmountConstraint() {
        UUID id = UUID.randomUUID();

        // Inserting negative amount must violate chk_payments_amount_positive
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO payments (id, customer_id, amount_cents, currency, status, version, created_at, updated_at) " +
                        "VALUES (?, 'cust_123', -500, 'USD', 'CREATED', 0, NOW(), NOW())",
                id
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Flyway enforces valid status enum check constraint")
    void shouldEnforceValidStatusConstraint() {
        UUID id = UUID.randomUUID();

        // Inserting invalid status must violate chk_payments_status_valid
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO payments (id, customer_id, amount_cents, currency, status, version, created_at, updated_at) " +
                        "VALUES (?, 'cust_123', 1000, 'USD', 'INVALID_STATUS', 0, NOW(), NOW())",
                id
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Flyway creates provider_transactions table successfully")
    void shouldVerifyProviderTransactionsTableExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'provider_transactions'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }
}
