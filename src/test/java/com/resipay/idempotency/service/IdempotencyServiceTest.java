package com.resipay.idempotency.service;

import com.resipay.common.AbstractPostgresIntegrationTest;
import com.resipay.idempotency.domain.IdempotencyRecord;
import com.resipay.idempotency.domain.IdempotencyStatus;
import com.resipay.idempotency.exception.IdempotencyConflictException;
import com.resipay.idempotency.exception.IdempotencyInProgressException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdempotencyServiceTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private IdempotencyService idempotencyService;

    @Test
    @DisplayName("Acquiring lock on a new key returns acquired status with IN_PROGRESS record")
    void shouldAcquireLockForNewKey() {
        String key = "key-" + UUID.randomUUID();
        LockResult result = idempotencyService.acquireLock(
                key, "POST", "/api/v1/payments", "hash123", Duration.ofHours(24), 500
        );

        assertThat(result.acquired()).isTrue();
        assertThat(result.replayed()).isFalse();
        assertThat(result.record().getStatus()).isEqualTo(IdempotencyStatus.IN_PROGRESS);
        assertThat(result.record().getIdempotencyKey()).isEqualTo(key);
    }

    @Test
    @DisplayName("Completing a record persists response status code and body")
    void shouldCompleteRecordSuccessfully() {
        String key = "key-" + UUID.randomUUID();
        idempotencyService.acquireLock(key, "POST", "/api/v1/payments", "hash123", Duration.ofHours(24), 500);

        idempotencyService.complete(key, 201, "{\"paymentId\":\"123\"}");

        IdempotencyRecord record = idempotencyService.findRecord(key).orElseThrow();
        assertThat(record.getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(record.getResponseCode()).isEqualTo(201);
        assertThat(record.getResponseBody()).isEqualTo("{\"paymentId\":\"123\"}");
    }

    @Test
    @DisplayName("Subsequent request with same key and identical hash returns replayed result")
    void shouldReturnReplayedForCompletedRecordWithSameHash() {
        String key = "key-" + UUID.randomUUID();
        idempotencyService.acquireLock(key, "POST", "/api/v1/payments", "hash123", Duration.ofHours(24), 500);
        idempotencyService.complete(key, 201, "{\"paymentId\":\"123\"}");

        LockResult result = idempotencyService.acquireLock(
                key, "POST", "/api/v1/payments", "hash123", Duration.ofHours(24), 500
        );

        assertThat(result.acquired()).isFalse();
        assertThat(result.replayed()).isTrue();
        assertThat(result.record().getStatus()).isEqualTo(IdempotencyStatus.COMPLETED);
        assertThat(result.record().getResponseBody()).isEqualTo("{\"paymentId\":\"123\"}");
    }

    @Test
    @DisplayName("Reusing an existing key with altered hash throws IdempotencyConflictException")
    void shouldThrowConflictExceptionForAlteredPayload() {
        String key = "key-" + UUID.randomUUID();
        idempotencyService.acquireLock(key, "POST", "/api/v1/payments", "original-hash", Duration.ofHours(24), 500);
        idempotencyService.complete(key, 201, "{\"paymentId\":\"123\"}");

        assertThatThrownBy(() -> idempotencyService.acquireLock(
                key, "POST", "/api/v1/payments", "altered-hash", Duration.ofHours(24), 500
        ))
                .isInstanceOf(IdempotencyConflictException.class)
                .hasMessageContaining("already been used with a different request payload");
    }

    @Test
    @DisplayName("Attempting to acquire lock while in progress throws IdempotencyInProgressException upon timeout")
    void shouldThrowInProgressExceptionWhenConcurrentLockWaitExceeded() {
        String key = "key-" + UUID.randomUUID();
        // First thread acquires lock
        idempotencyService.acquireLock(key, "POST", "/api/v1/payments", "hash-wait", Duration.ofHours(24), 500);

        // Second thread with same key and hash waits up to 200ms and times out
        assertThatThrownBy(() -> idempotencyService.acquireLock(
                key, "POST", "/api/v1/payments", "hash-wait", Duration.ofHours(24), 200
        ))
                .isInstanceOf(IdempotencyInProgressException.class)
                .hasMessageContaining("currently in progress");
    }
}
