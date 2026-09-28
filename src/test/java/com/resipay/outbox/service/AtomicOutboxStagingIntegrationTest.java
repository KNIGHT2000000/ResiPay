package com.resipay.outbox.service;

import com.resipay.common.AbstractPostgresIntegrationTest;
import com.resipay.outbox.domain.OutboxEvent;
import com.resipay.outbox.domain.OutboxStatus;
import com.resipay.outbox.repository.OutboxRepository;
import com.resipay.payment.api.dto.CreatePaymentRequest;
import com.resipay.payment.api.dto.PaymentResponse;
import com.resipay.payment.domain.PaymentStatus;
import com.resipay.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AtomicOutboxStagingIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OutboxRepository outboxRepository;

    @Test
    @DisplayName("Creating a payment atomically writes a PAYMENT_CREATED outbox event")
    void shouldStageOutboxEventOnPaymentCreation() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .customerId("cust_outbox_test")
                .amountCents(12000L)
                .currency("USD")
                .idempotencyKey("idem-outbox-" + System.currentTimeMillis())
                .build();

        PaymentResponse response = paymentService.createPayment(request);

        List<OutboxEvent> events = outboxRepository.findByAggregateId(response.id().toString());
        assertThat(events).hasSize(1);

        OutboxEvent event = events.getFirst();
        assertThat(event.getAggregateType()).isEqualTo("PAYMENT");
        assertThat(event.getEventType()).isEqualTo("PAYMENT_CREATED");
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getPayload()).contains("12000");
    }

    @Test
    @DisplayName("Transitioning a payment status stages a PAYMENT_TRANSITIONED outbox event")
    void shouldStageOutboxEventOnPaymentTransition() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .customerId("cust_outbox_trans")
                .amountCents(7500L)
                .currency("USD")
                .idempotencyKey("idem-trans-" + System.currentTimeMillis())
                .build();

        PaymentResponse created = paymentService.createPayment(request);

        // Transition from CREATED to VALIDATED
        paymentService.transitionPayment(created.id(), PaymentStatus.VALIDATED, null, null);

        List<OutboxEvent> events = outboxRepository.findByAggregateId(created.id().toString());

        assertThat(events).hasSize(2);
        assertThat(events.get(0).getEventType()).isEqualTo("PAYMENT_CREATED");
        assertThat(events.get(1).getEventType()).isEqualTo("PAYMENT_TRANSITIONED");
        assertThat(events.get(1).getPayload()).contains("VALIDATED");
    }
}

