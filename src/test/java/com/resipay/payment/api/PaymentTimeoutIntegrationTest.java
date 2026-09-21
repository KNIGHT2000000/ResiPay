package com.resipay.payment.api;

import com.resipay.common.AbstractPostgresIntegrationTest;
import com.resipay.payment.api.dto.PaymentResponse;
import com.resipay.payment.domain.Payment;
import com.resipay.payment.domain.PaymentStatus;
import com.resipay.payment.repository.PaymentRepository;
import com.resipay.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentTimeoutIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("STATE-03: Ambiguous provider timeout transitions payment to UNKNOWN status instead of FAILED")
    void shouldTransitionToUnknownOnDownstreamTimeout() {
        // 1. Create a payment in PROCESSING state
        Payment payment = Payment.builder()
                .customerId("cust_timeout_test")
                .amountCents(25000L)
                .currency("USD")
                .status(PaymentStatus.PROCESSING)
                .build();
        payment = paymentRepository.saveAndFlush(payment);
        UUID paymentId = payment.getId();

        // 2. Simulate downstream socket/read timeout interceptor
        PaymentResponse timeoutResponse = paymentService.handleDownstreamTimeout(
                paymentId,
                "Read timed out waiting for provider gateway acknowledgement"
        );

        // 3. Verify state transition to UNKNOWN
        assertThat(timeoutResponse.status()).isEqualTo(PaymentStatus.UNKNOWN);
        assertThat(timeoutResponse.errorCode()).isEqualTo("DOWNSTREAM_TIMEOUT_AMBIGUOUS");
        assertThat(timeoutResponse.errorMessage()).contains("Read timed out");

        // 4. Verify persistence in PostgreSQL
        Payment persisted = paymentRepository.findById(paymentId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(PaymentStatus.UNKNOWN);
        assertThat(persisted.getStatus()).isNotEqualTo(PaymentStatus.FAILED);
    }
}
