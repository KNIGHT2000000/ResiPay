package com.resipay.payment.service;

import com.resipay.payment.api.dto.CreatePaymentRequest;
import com.resipay.payment.api.dto.PaymentResponse;
import com.resipay.payment.domain.InvalidStateTransitionException;
import com.resipay.payment.domain.Payment;
import com.resipay.payment.domain.PaymentStateMachine;
import com.resipay.payment.domain.PaymentStatus;
import com.resipay.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private com.resipay.outbox.service.OutboxStagingService outboxStagingService;

    private PaymentStateMachine stateMachine;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        stateMachine = new PaymentStateMachine();
        paymentService = new PaymentService(paymentRepository, stateMachine, outboxStagingService);
    }


    @Test
    @DisplayName("createPayment initializes payment in CREATED status")
    void shouldCreatePaymentInCreatedStatus() {
        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .customerId("cust_123")
                .amountCents(5000L)
                .currency("USD")
                .idempotencyKey("idem_key_1")
                .build();

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            p.setVersion(0L);
            return p;
        });

        PaymentResponse response = paymentService.createPayment(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(PaymentStatus.CREATED);
        assertThat(response.amountCents()).isEqualTo(5000L);
        assertThat(response.currency()).isEqualTo("USD");

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PaymentStatus.CREATED);
    }

    @Test
    @DisplayName("transitionPayment rejects illegal transition")
    void shouldRejectIllegalTransition() {
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.FAILED)
                .customerId("cust_123")
                .amountCents(1000L)
                .currency("USD")
                .version(0L)
                .build();

        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // FAILED -> COMPLETED is illegal
        assertThatThrownBy(() -> paymentService.transitionPayment(paymentId, PaymentStatus.COMPLETED, null, null))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
