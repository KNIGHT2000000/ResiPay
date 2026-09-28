package com.resipay.outbox.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransitionedEvent implements PaymentEvent {

    @Builder.Default
    private UUID eventId = UUID.randomUUID();

    private UUID paymentId;
    private String previousStatus;
    private String targetStatus;
    private String errorCode;
    private String errorMessage;

    @Builder.Default
    private String eventType = "PAYMENT_TRANSITIONED";

    @Builder.Default
    private Instant occurredAt = Instant.now();

    @Builder.Default
    private int schemaVersion = 1;
}
