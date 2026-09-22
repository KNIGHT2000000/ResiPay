package com.resipay.idempotency.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PayloadHasherTest {

    @Test
    @DisplayName("Identical inputs produce identical SHA-256 hash")
    void shouldProduceConsistentHashForIdenticalInputs() {
        String hash1 = PayloadHasher.computeHash("POST", "/api/v1/payments", "{\"amountCents\": 1000}");
        String hash2 = PayloadHasher.computeHash("POST", "/api/v1/payments", "{\"amountCents\": 1000}");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64);
    }

    @Test
    @DisplayName("Different bodies produce distinct SHA-256 hashes")
    void shouldProduceDistinctHashForDifferentBody() {
        String hash1 = PayloadHasher.computeHash("POST", "/api/v1/payments", "{\"amountCents\": 1000}");
        String hash2 = PayloadHasher.computeHash("POST", "/api/v1/payments", "{\"amountCents\": 2000}");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("Different paths produce distinct SHA-256 hashes")
    void shouldProduceDistinctHashForDifferentPath() {
        String hash1 = PayloadHasher.computeHash("POST", "/api/v1/payments", "{}");
        String hash2 = PayloadHasher.computeHash("POST", "/api/v1/refunds", "{}");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("Null and empty inputs are handled safely without throwing NullPointerException")
    void shouldHandleNullAndEmptyInputsSafely() {
        String hash = PayloadHasher.computeHash(null, null, null);
        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(64);
    }
}
