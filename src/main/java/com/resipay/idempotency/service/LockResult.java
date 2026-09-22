package com.resipay.idempotency.service;

import com.resipay.idempotency.domain.IdempotencyRecord;

public record LockResult(
        boolean acquired,
        boolean replayed,
        IdempotencyRecord record
) {
    public static LockResult acquired(IdempotencyRecord record) {
        return new LockResult(true, false, record);
    }

    public static LockResult replayed(IdempotencyRecord record) {
        return new LockResult(false, true, record);
    }
}
