package com.resipay.payment.domain;

import com.resipay.common.AbstractPostgresIntegrationTest;
import com.resipay.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEntityTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("Payment entity persists with integer minor units and versioning")
    @Transactional
    void shouldPersistAndRetrievePayment() {
        Payment payment = Payment.builder()
                .customerId("cust_test_99")
                .amountCents(15000L) // $150.00
                .currency("USD")
                .status(PaymentStatus.CREATED)
                .build();

        Payment saved = paymentRepository.saveAndFlush(payment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(0L);
        assertThat(saved.getAmountCents()).isEqualTo(15000L);
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.CREATED);
        assertThat(saved.getCreatedAt()).isNotNull();

        // Update payment to VALIDATED
        saved.setStatus(PaymentStatus.VALIDATED);
        Payment updated = paymentRepository.saveAndFlush(saved);

        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.VALIDATED);
    }
}
