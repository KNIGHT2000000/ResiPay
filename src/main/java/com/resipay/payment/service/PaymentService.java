package com.resipay.payment.service;

import com.resipay.payment.api.dto.CreatePaymentRequest;
import com.resipay.payment.api.dto.PaymentResponse;
import com.resipay.payment.domain.Payment;
import com.resipay.payment.domain.PaymentStateMachine;
import com.resipay.payment.domain.PaymentStatus;
import com.resipay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine stateMachine;

    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        log.info("Creating payment for customer: {}, amount: {} {}",
                request.getCustomerId(), request.getAmountCents(), request.getCurrency());

        Payment payment = Payment.builder()
                .customerId(request.getCustomerId())
                .amountCents(request.getAmountCents())
                .currency(request.getCurrency())
                .idempotencyKey(request.getIdempotencyKey())
                .status(PaymentStatus.CREATED)
                .build();

        Payment saved = paymentRepository.save(payment);
        return PaymentResponse.from(saved);
    }

    @Transactional
    public PaymentResponse transitionPayment(UUID paymentId, PaymentStatus targetStatus, String errorCode, String errorMessage) {
        Payment payment = getPaymentEntity(paymentId);
        PaymentStatus currentStatus = payment.getStatus();

        stateMachine.validateTransition(currentStatus, targetStatus);

        log.info("Transitioning payment {} from {} to {}", paymentId, currentStatus, targetStatus);
        payment.setStatus(targetStatus);
        if (errorCode != null) {
            payment.setErrorCode(errorCode);
        }
        if (errorMessage != null) {
            payment.setErrorMessage(errorMessage);
        }

        Payment updated = paymentRepository.save(payment);
        return PaymentResponse.from(updated);
    }

    @Transactional
    public PaymentResponse handleDownstreamTimeout(UUID paymentId, String detailMessage) {
        Payment payment = getPaymentEntity(paymentId);
        PaymentStatus currentStatus = payment.getStatus();

        log.warn("Downstream network timeout encountered for payment {}. Transitioning to UNKNOWN status (never FAILED). Detail: {}",
                paymentId, detailMessage);

        stateMachine.validateTransition(currentStatus, PaymentStatus.UNKNOWN);
        payment.setStatus(PaymentStatus.UNKNOWN);
        payment.setErrorCode("DOWNSTREAM_TIMEOUT_AMBIGUOUS");
        payment.setErrorMessage(detailMessage != null ? detailMessage : "Provider call timed out without response acknowledgement");

        Payment updated = paymentRepository.save(payment);
        return PaymentResponse.from(updated);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        return PaymentResponse.from(getPaymentEntity(paymentId));
    }

    @Transactional(readOnly = true)
    public Payment getPaymentEntity(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found: " + paymentId));
    }
}
