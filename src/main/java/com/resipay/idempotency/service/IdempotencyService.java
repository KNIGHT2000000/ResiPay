package com.resipay.idempotency.service;

import com.resipay.idempotency.domain.IdempotencyRecord;
import com.resipay.idempotency.domain.IdempotencyStatus;
import com.resipay.idempotency.exception.IdempotencyConflictException;
import com.resipay.idempotency.exception.IdempotencyInProgressException;
import com.resipay.idempotency.repository.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final PlatformTransactionManager transactionManager;

    /**
     * Atomically acquires an execution lock for the given idempotency key.
     * If the key already exists:
     * - If payload hash does not match, throws IdempotencyConflictException (HTTP 422).
     * - If completed, returns LockResult.replayed with the cached record.
     * - If in progress, polls up to waitTimeoutMs for completion. If still in progress, throws IdempotencyInProgressException (HTTP 409).
     */
    public LockResult acquireLock(
            String key,
            String method,
            String path,
            String payloadHash,
            Duration ttl,
            long waitTimeoutMs
    ) {
        log.debug("Attempting to acquire idempotency lock for key: {}", key);

        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            IdempotencyRecord newRecord = requiresNew.execute(status -> {
                Instant now = Instant.now();
                IdempotencyRecord record = IdempotencyRecord.builder()
                        .id(UUID.randomUUID())
                        .idempotencyKey(key)
                        .requestHash(payloadHash)
                        .requestPath(path)
                        .requestMethod(method)
                        .status(IdempotencyStatus.IN_PROGRESS)
                        .createdAt(now)
                        .updatedAt(now)
                        .lockedAt(now)
                        .expiresAt(now.plus(ttl))
                        .build();
                return idempotencyRepository.saveAndFlush(record);
            });
            log.info("Acquired new idempotency lock for key: {}", key);
            return LockResult.acquired(newRecord);
        } catch (DataIntegrityViolationException e) {
            log.debug("Idempotency key '{}' already exists in database. Inspecting existing record.", key);
            return handleExistingRecord(key, payloadHash, waitTimeoutMs);
        }
    }

    private LockResult handleExistingRecord(String key, String payloadHash, long waitTimeoutMs) {
        IdempotencyRecord existing = findRecord(key)
                .orElseThrow(() -> new IllegalStateException("Idempotency record not found after unique constraint hit: " + key));

        // 1. Conflict detection (IDEM-02)
        if (!existing.getRequestHash().equals(payloadHash)) {
            log.warn("Idempotency conflict detected for key '{}'. Existing hash: {}, incoming hash: {}",
                    key, existing.getRequestHash(), payloadHash);
            throw new IdempotencyConflictException(
                    key,
                    "Idempotency key '" + key + "' has already been used with a different request payload."
            );
        }

        // 2. Already completed (IDEM-01)
        if (existing.getStatus() == IdempotencyStatus.COMPLETED) {
            log.info("Idempotency key '{}' already completed. Returning cached response.", key);
            return LockResult.replayed(existing);
        }

        // 3. In-progress concurrency guard (IDEM-03)
        if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            long deadline = System.currentTimeMillis() + waitTimeoutMs;
            long pollIntervalMs = 50;

            log.info("Concurrent execution detected for key '{}'. Waiting up to {}ms for in-progress completion.",
                    key, waitTimeoutMs);

            while (System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(pollIntervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IdempotencyInProgressException(key, "Thread interrupted while waiting for idempotency lock.");
                }

                Optional<IdempotencyRecord> refreshed = findRecord(key);
                if (refreshed.isPresent()) {
                    IdempotencyRecord rec = refreshed.get();
                    if (rec.getStatus() == IdempotencyStatus.COMPLETED) {
                        log.info("In-progress request for key '{}' completed while waiting. Replaying cached response.", key);
                        return LockResult.replayed(rec);
                    }
                }
            }

            log.warn("Wait timeout exceeded for in-progress idempotency key '{}'. Rejecting concurrent execution.", key);
            throw new IdempotencyInProgressException(
                    key,
                    "A concurrent request with idempotency key '" + key + "' is currently in progress."
            );
        }

        // 4. Failed previously - allow retry by taking over the lock
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        IdempotencyRecord updated = requiresNew.execute(status -> {
            IdempotencyRecord rec = findRecord(key).orElse(existing);
            rec.setStatus(IdempotencyStatus.IN_PROGRESS);
            rec.setLockedAt(Instant.now());
            rec.setUpdatedAt(Instant.now());
            return idempotencyRepository.saveAndFlush(rec);
        });

        return LockResult.acquired(updated);
    }

    public void complete(String key, int responseCode, String responseBody) {
        log.info("Completing idempotency record for key: {}, responseCode: {}", key, responseCode);
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        requiresNew.executeWithoutResult(status -> {
            idempotencyRepository.findByIdempotencyKey(key).ifPresent(record -> {
                record.setStatus(IdempotencyStatus.COMPLETED);
                record.setResponseCode(responseCode);
                record.setResponseBody(responseBody);
                record.setUpdatedAt(Instant.now());
                idempotencyRepository.saveAndFlush(record);
            });
        });
    }

    public void fail(String key) {
        log.warn("Marking idempotency record as FAILED for key: {}", key);
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        requiresNew.executeWithoutResult(status -> {
            idempotencyRepository.findByIdempotencyKey(key).ifPresent(record -> {
                record.setStatus(IdempotencyStatus.FAILED);
                record.setUpdatedAt(Instant.now());
                idempotencyRepository.saveAndFlush(record);
            });
        });
    }

    public Optional<IdempotencyRecord> findRecord(String key) {
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        requiresNew.setReadOnly(true);
        return requiresNew.execute(status -> idempotencyRepository.findByIdempotencyKey(key));
    }
}
