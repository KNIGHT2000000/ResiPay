package com.resipay.payment.api.dto;

import com.resipay.payment.domain.Payment;
import com.resipay.payment.domain.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String customerId,
        Long amountCents,
        String currency,
        PaymentStatus status,
        String idempotencyKey,
        Long version,
        Instant createdAt,
        Instant updatedAt,
        String errorCode,
        String errorMessage
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getCustomerId(),
                payment.getAmountCents(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getIdempotencyKey(),
                payment.getVersion(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getErrorCode(),
                payment.getErrorMessage()
        );
    }
}
