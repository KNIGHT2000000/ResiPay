package com.resipay.idempotency.web;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyConcurrencyIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    @DisplayName("IDEM-03: High-concurrency stress test with 20 simultaneous threads produces exactly 1 payment and zero duplicates")
    void shouldPreventDuplicateProcessingUnderHighConcurrency() throws InterruptedException {
        int threadCount = 20;
        String idempotencyKey = "concurrent-key-" + UUID.randomUUID();
        String customerId = "cust_concurrent_" + UUID.randomUUID();

        CreatePaymentRequest request = CreatePaymentRequest.builder()
                .customerId(customerId)
                .amountCents(50000L)
                .currency("USD")
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(IdempotencyFilter.IDEMPOTENCY_KEY_HEADER, idempotencyKey);

        HttpEntity<CreatePaymentRequest> entity = new HttpEntity<>(request, headers);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        List<ResponseEntity<String>> responses = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await(); // wait for simultaneous release
                    ResponseEntity<String> response = restTemplate.exchange(
                            "/api/v1/payments", HttpMethod.POST, entity, String.class
                    );
                    responses.add(response);
                } catch (Exception e) {
                    // ignore
                } finally {
                    doneGate.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startGate.countDown();
        boolean completed = doneGate.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(responses).hasSize(threadCount);

        // All responses must be either 201 Created or 409 Conflict (never 500 or unhandled)
        long createdCount = responses.stream()
                .filter(r -> r.getStatusCode() == HttpStatus.CREATED)
                .count();
        long conflictCount = responses.stream()
                .filter(r -> r.getStatusCode() == HttpStatus.CONFLICT)
                .count();

        assertThat(createdCount).isGreaterThanOrEqualTo(1L);
        assertThat(createdCount + conflictCount).isEqualTo(threadCount);

        // Core Financial Invariant: Exactly 1 payment row in database! Zero duplicate records!
        long dbPaymentCount = paymentRepository.findAll().stream()
                .filter(p -> customerId.equals(p.getCustomerId()))
                .count();
        assertThat(dbPaymentCount).isEqualTo(1L);
    }
}
