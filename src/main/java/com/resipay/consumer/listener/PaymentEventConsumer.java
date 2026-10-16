package com.resipay.consumer.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resipay.consumer.service.IdempotentConsumerHandler;
import com.resipay.outbox.config.KafkaTopicConfig;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    public static final String CONSUMER_GROUP = "resipay-payment-processor";

    private final IdempotentConsumerHandler idempotentConsumerHandler;
    private final ObjectMapper objectMapper;
    private final com.resipay.ledger.service.PaymentLedgerPostingHandler paymentLedgerPostingHandler;

    @Getter
    private final AtomicInteger processedCount = new AtomicInteger(0);

    @Getter
    private final AtomicInteger duplicateSkippedCount = new AtomicInteger(0);

    @KafkaListener(
            topics = KafkaTopicConfig.PAYMENT_EVENTS_TOPIC,
            groupId = CONSUMER_GROUP,
            autoStartup = "${resipay.kafka.listener.auto-startup:false}"
    )
    public void onPaymentEvent(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventIdStr = root.path("eventId").asText();
            String paymentIdStr = root.path("paymentId").asText();
            String eventType = root.path("eventType").asText("UNKNOWN_EVENT");

            if (eventIdStr.isEmpty() || paymentIdStr.isEmpty()) {
                log.warn("Skipping unparseable payment event: {}", payload);
                return;
            }

            UUID eventId = UUID.fromString(eventIdStr);

            boolean processed = idempotentConsumerHandler.processIdempotently(
                    eventId,
                    eventType,
                    paymentIdStr,
                    CONSUMER_GROUP,
                    () -> {
                        log.info("Consumer executing downstream ledger handler for payment: {}", paymentIdStr);
                        paymentLedgerPostingHandler.handlePaymentEvent(root);
                    }
            );

            if (processed) {
                processedCount.incrementAndGet();
            } else {
                duplicateSkippedCount.incrementAndGet();
            }
        } catch (Exception e) {
            log.error("Error processing incoming Kafka message: {}", e.getMessage(), e);
        }
    }
}
