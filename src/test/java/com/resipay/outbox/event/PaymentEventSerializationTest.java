package com.resipay.outbox.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEventSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("PaymentCreatedEvent serializes to canonical JSON schema with version")
    void shouldSerializePaymentCreatedEvent() throws Exception {
        UUID paymentId = UUID.randomUUID();
        PaymentCreatedEvent event = PaymentCreatedEvent.builder()
                .paymentId(paymentId)
                .customerId("cust_123")
                .amountCents(5000L)
                .currency("USD")
                .idempotencyKey("idem-xyz")
                .build();

        String json = objectMapper.writeValueAsString(event);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("paymentId").asText()).isEqualTo(paymentId.toString());
        assertThat(node.get("customerId").asText()).isEqualTo("cust_123");
        assertThat(node.get("amountCents").asLong()).isEqualTo(5000L);
        assertThat(node.get("currency").asText()).isEqualTo("USD");
        assertThat(node.get("eventType").asText()).isEqualTo("PAYMENT_CREATED");
        assertThat(node.get("schemaVersion").asInt()).isEqualTo(1);
    }

    @Test
    @DisplayName("PaymentTransitionedEvent serializes state transition details")
    void shouldSerializePaymentTransitionedEvent() throws Exception {
        UUID paymentId = UUID.randomUUID();
        PaymentTransitionedEvent event = PaymentTransitionedEvent.builder()
                .paymentId(paymentId)
                .previousStatus("CREATED")
                .targetStatus("PROCESSING")
                .build();

        String json = objectMapper.writeValueAsString(event);
        JsonNode node = objectMapper.readTree(json);

        assertThat(node.get("paymentId").asText()).isEqualTo(paymentId.toString());
        assertThat(node.get("previousStatus").asText()).isEqualTo("CREATED");
        assertThat(node.get("targetStatus").asText()).isEqualTo("PROCESSING");
        assertThat(node.get("eventType").asText()).isEqualTo("PAYMENT_TRANSITIONED");
    }
}
