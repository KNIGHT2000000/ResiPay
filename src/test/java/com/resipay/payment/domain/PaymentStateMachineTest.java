package com.resipay.payment.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStateMachineTest {

    private PaymentStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new PaymentStateMachine();
    }

    @ParameterizedTest(name = "Valid transition from {0} to {1}")
    @CsvSource({
            "CREATED, VALIDATED",
            "CREATED, FAILED",
            "CREATED, CANCELLED",
            "VALIDATED, PROCESSING",
            "PROCESSING, AUTHORIZED",
            "PROCESSING, COMPLETED",
            "PROCESSING, DECLINED",
            "PROCESSING, FAILED",
            "PROCESSING, UNKNOWN",
            "AUTHORIZED, COMPLETED",
            "AUTHORIZED, REFUND_PENDING",
            "UNKNOWN, COMPLETED",
            "UNKNOWN, FAILED",
            "UNKNOWN, DECLINED",
            "UNKNOWN, REFUNDED",
            "COMPLETED, REFUND_PENDING",
            "REFUND_PENDING, REFUNDED"
    })
    void shouldAllowValidTransitions(PaymentStatus from, PaymentStatus to) {
        assertThat(stateMachine.canTransition(from, to)).isTrue();
        assertThatCode(() -> stateMachine.validateTransition(from, to)).doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "Illegal transition from {0} to {1} must throw exception")
    @CsvSource({
            "FAILED, COMPLETED",
            "FAILED, PROCESSING",
            "DECLINED, COMPLETED",
            "REFUNDED, COMPLETED",
            "COMPLETED, PROCESSING",
            "CREATED, COMPLETED",
            "CREATED, AUTHORIZED",
            "VALIDATED, COMPLETED"
    })
    void shouldRejectIllegalTransitions(PaymentStatus from, PaymentStatus to) {
        assertThat(stateMachine.canTransition(from, to)).isFalse();
        assertThatThrownBy(() -> stateMachine.validateTransition(from, to))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining(String.format("from %s to %s", from, to));
    }

    @Test
    @DisplayName("Terminal states have zero permitted outgoing transitions")
    void shouldEnforceTerminalStates() {
        for (PaymentStatus next : PaymentStatus.values()) {
            assertThat(stateMachine.canTransition(PaymentStatus.FAILED, next)).isFalse();
            assertThat(stateMachine.canTransition(PaymentStatus.DECLINED, next)).isFalse();
            assertThat(stateMachine.canTransition(PaymentStatus.REFUNDED, next)).isFalse();
            assertThat(stateMachine.canTransition(PaymentStatus.CANCELLED, next)).isFalse();
        }
    }

    @Test
    @DisplayName("UNKNOWN state explicitly permits resolution transitions")
    void shouldAllowUnknownResolutionTransitions() {
        assertThat(stateMachine.canTransition(PaymentStatus.UNKNOWN, PaymentStatus.COMPLETED)).isTrue();
        assertThat(stateMachine.canTransition(PaymentStatus.UNKNOWN, PaymentStatus.FAILED)).isTrue();
        assertThat(stateMachine.canTransition(PaymentStatus.UNKNOWN, PaymentStatus.DECLINED)).isTrue();
        assertThat(stateMachine.canTransition(PaymentStatus.UNKNOWN, PaymentStatus.REFUNDED)).isTrue();
    }
}
