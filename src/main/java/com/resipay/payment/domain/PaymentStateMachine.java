package com.resipay.payment.domain;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Formal state machine governing lifecycle transitions for ResiPay payments.
 */
@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS;

    static {
        Map<PaymentStatus, Set<PaymentStatus>> transitions = new EnumMap<>(PaymentStatus.class);

        transitions.put(PaymentStatus.CREATED, EnumSet.of(
                PaymentStatus.VALIDATED,
                PaymentStatus.FAILED,
                PaymentStatus.CANCELLED
        ));

        transitions.put(PaymentStatus.VALIDATED, EnumSet.of(
                PaymentStatus.PROCESSING,
                PaymentStatus.FAILED,
                PaymentStatus.CANCELLED
        ));

        transitions.put(PaymentStatus.PROCESSING, EnumSet.of(
                PaymentStatus.AUTHORIZED,
                PaymentStatus.COMPLETED,
                PaymentStatus.DECLINED,
                PaymentStatus.FAILED,
                PaymentStatus.UNKNOWN
        ));

        transitions.put(PaymentStatus.AUTHORIZED, EnumSet.of(
                PaymentStatus.COMPLETED,
                PaymentStatus.FAILED,
                PaymentStatus.REFUND_PENDING,
                PaymentStatus.CANCELLED
        ));

        // UNKNOWN can transition to terminal states or refund upon manual/automated reconciliation
        transitions.put(PaymentStatus.UNKNOWN, EnumSet.of(
                PaymentStatus.COMPLETED,
                PaymentStatus.FAILED,
                PaymentStatus.DECLINED,
                PaymentStatus.REFUNDED
        ));

        transitions.put(PaymentStatus.COMPLETED, EnumSet.of(
                PaymentStatus.REFUND_PENDING
        ));

        transitions.put(PaymentStatus.REFUND_PENDING, EnumSet.of(
                PaymentStatus.REFUNDED,
                PaymentStatus.COMPLETED
        ));

        // Terminal states: FAILED, DECLINED, REFUNDED, CANCELLED
        transitions.put(PaymentStatus.FAILED, Collections.emptySet());
        transitions.put(PaymentStatus.DECLINED, Collections.emptySet());
        transitions.put(PaymentStatus.REFUNDED, Collections.emptySet());
        transitions.put(PaymentStatus.CANCELLED, Collections.emptySet());

        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    /**
     * Checks whether a transition between two states is permitted.
     */
    public boolean canTransition(PaymentStatus from, PaymentStatus to) {
        if (from == null || to == null) {
            return false;
        }
        Set<PaymentStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Collections.emptySet());
        return allowed.contains(to);
    }

    /**
     * Validates that a transition between two states is permitted.
     * Throws InvalidStateTransitionException if invalid.
     */
    public void validateTransition(PaymentStatus from, PaymentStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStateTransitionException(from, to);
        }
    }
}
