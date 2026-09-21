package com.resipay.simulator.dto;

import java.time.Instant;
import java.util.UUID;

public record SimulatorChargeResponse(
        UUID transactionId,
        UUID paymentId,
        String externalReference,
        String status,
        Long amountCents,
        String currency,
        SimulationOutcome outcome,
        String declineReason,
        Instant processedAt
) {
    public static SimulatorChargeResponse success(UUID txId, UUID paymentId, String externalRef, Long amount, String currency) {
        return new SimulatorChargeResponse(txId, paymentId, externalRef, "CAPTURED", amount, currency, SimulationOutcome.SUCCESS, null, Instant.now());
    }

    public static SimulatorChargeResponse declined(UUID txId, UUID paymentId, String externalRef, Long amount, String currency, String reason) {
        return new SimulatorChargeResponse(txId, paymentId, externalRef, "DECLINED", amount, currency, SimulationOutcome.DECLINED, reason, Instant.now());
    }
}
