package com.resipay.payment.domain;

/**
 * Formal state enumeration for the ResiPay payment lifecycle.
 */
public enum PaymentStatus {
    CREATED,
    VALIDATED,
    PROCESSING,
    AUTHORIZED,
    COMPLETED,
    FAILED,
    DECLINED,
    UNKNOWN,
    CANCELLED,
    REFUND_PENDING,
    REFUNDED;

    /**
     * Determines whether the current state is considered terminal.
     * Terminal states accept no further outgoing transitions.
     */
    public boolean isTerminal() {
        return this == FAILED || this == DECLINED || this == REFUNDED;
    }

    /**
     * Indicates whether the payment state represents an ambiguous outcome
     * requiring out-of-band resolution or reconciliation.
     */
    public boolean isAmbiguous() {
        return this == UNKNOWN;
    }
}
