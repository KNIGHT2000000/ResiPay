package com.resipay.idempotency.exception;

public class IdempotencyInProgressException extends RuntimeException {

    private final String idempotencyKey;

    public IdempotencyInProgressException(String idempotencyKey, String message) {
        super(message);
        this.idempotencyKey = idempotencyKey;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}
