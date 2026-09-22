package com.resipay.payment.api;

import com.resipay.payment.api.dto.CreatePaymentRequest;
import com.resipay.payment.api.dto.PaymentResponse;
import com.resipay.payment.domain.PaymentStatus;
import com.resipay.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKeyHeader,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        if (request.getIdempotencyKey() == null && idempotencyKeyHeader != null) {
            request.setIdempotencyKey(idempotencyKeyHeader);
        }
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
        PaymentResponse response = paymentService.getPayment(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/transition")
    public ResponseEntity<PaymentResponse> transition(
            @PathVariable UUID id,
            @RequestParam PaymentStatus targetStatus,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String errorMessage
    ) {
        PaymentResponse response = paymentService.transitionPayment(id, targetStatus, errorCode, errorMessage);
        return ResponseEntity.ok(response);
    }
}
