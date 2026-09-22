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

class IdempotentReplayIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("IDEM-01: Replaying identical payment request returns cached response without duplicate side effects")
    void shouldReplayCachedResponseForIdenticalRequest() throws Exception {
        String idempotencyKey = "replay-key-" + UUID.randomUUID();
        String customerId = "cust_replay_" + UUID.randomUUID();

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .customerId(customerId)
                .amountCents(15000L)
                .currency("USD")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(IdempotencyFilter.IDEMPOTENCY_KEY_HEADER, idempotencyKey);

        HttpEntity<CreatePaymentRequest> entity = new HttpEntity<>(request, headers);

        // 1. Initial request execution
        ResponseEntity<String> firstResponse = restTemplate.exchange(
                "/api/v1/payments", HttpMethod.POST, entity, String.class
        );

        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(firstResponse.getHeaders().get(IdempotencyFilter.IDEMPOTENT_REPLAYED_HEADER)).isNull();

        JsonNode firstJson = objectMapper.readTree(firstResponse.getBody());
        String paymentId = firstJson.get("id").asText();
        assertThat(paymentId).isNotNull();

        // 2. Replay with identical key and identical payload
        ResponseEntity<String> secondResponse = restTemplate.exchange(
                "/api/v1/payments", HttpMethod.POST, entity, String.class
        );

        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondResponse.getHeaders().getFirst(IdempotencyFilter.IDEMPOTENT_REPLAYED_HEADER))
                .isEqualTo("true");

        JsonNode secondJson = objectMapper.readTree(secondResponse.getBody());
        assertThat(secondJson.get("id").asText()).isEqualTo(paymentId);
        assertThat(secondJson.get("amountCents").asLong()).isEqualTo(15000L);

        // 3. Invariant check: only 1 payment entity created in DB for customer
        long count = paymentRepository.findAll().stream()
                .filter(p -> customerId.equals(p.getCustomerId()))
                .count();
        assertThat(count).isEqualTo(1L);
    }
}
