package com.resipay.idempotency.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resipay.common.AbstractPostgresIntegrationTest;
import com.resipay.payment.api.dto.CreatePaymentRequest;
import com.resipay.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotentConflictIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("IDEM-02: Reusing existing idempotency key with altered payload is rejected with HTTP 422")
    void shouldRejectAlteredPayloadWithHttp422() throws Exception {
        String idempotencyKey = "conflict-key-" + UUID.randomUUID();
        String customerId = "cust_conflict_" + UUID.randomUUID();

        CreatePaymentRequest initialRequest = CreatePaymentRequest.builder()
                .customerId(customerId)
                .amountCents(10000L)
                .currency("USD")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(IdempotencyFilter.IDEMPOTENCY_KEY_HEADER, idempotencyKey);

        // 1. First request succeeds with HTTP 201
        ResponseEntity<String> firstResponse = restTemplate.exchange(
                "/api/v1/payments", HttpMethod.POST, new HttpEntity<>(initialRequest, headers), String.class
        );
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 2. Second request reuses same key but modifies amount (10000 -> 25000)
        CreatePaymentRequest alteredAmountRequest = CreatePaymentRequest.builder()
                .customerId(customerId)
                .amountCents(25000L)
                .currency("USD")
                .build();

        ResponseEntity<String> conflictResponse1 = restTemplate.exchange(
                "/api/v1/payments", HttpMethod.POST, new HttpEntity<>(alteredAmountRequest, headers), String.class
        );

        assertThat(conflictResponse1.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        JsonNode conflictJson1 = objectMapper.readTree(conflictResponse1.getBody());
        assertThat(conflictJson1.get("status").asInt()).isEqualTo(422);
        assertThat(conflictJson1.get("message").asText()).contains("different request payload");
        assertThat(conflictJson1.get("idempotencyKey").asText()).isEqualTo(idempotencyKey);

        // 3. Third request reuses same key but modifies currency (USD -> EUR)
        CreatePaymentRequest alteredCurrencyRequest = CreatePaymentRequest.builder()
                .customerId(customerId)
                .amountCents(10000L)
                .currency("EUR")
                .build();

        ResponseEntity<String> conflictResponse2 = restTemplate.exchange(
                "/api/v1/payments", HttpMethod.POST, new HttpEntity<>(alteredCurrencyRequest, headers), String.class
        );

        assertThat(conflictResponse2.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

        // 4. Invariant check: only 1 payment entity created in DB for customer
        long count = paymentRepository.findAll().stream()
                .filter(p -> customerId.equals(p.getCustomerId()))
                .count();
        assertThat(count).isEqualTo(1L);
    }
}
