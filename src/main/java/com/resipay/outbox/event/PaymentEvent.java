package com.resipay.outbox.event;

import java.time.Instant;
import java.util.UUID;

public interface PaymentEvent {
    UUID getEventId();
    UUID getPaymentId();
    String getEventType();
    Instant getOccurredAt();
    int getSchemaVersion();
}
