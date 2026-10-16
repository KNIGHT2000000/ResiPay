package com.resipay.outbox.publisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resipay.kafka.KafkaAvailabilityService;
import com.resipay.ledger.service.PaymentLedgerPostingHandler;
import com.resipay.outbox.config.KafkaTopicConfig;
import com.resipay.outbox.domain.OutboxEvent;
import com.resipay.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherService {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaAvailabilityService kafkaAvailabilityService;
    private final PaymentLedgerPostingHandler paymentLedgerPostingHandler;
    private final ObjectMapper objectMapper;

    @Value("${resipay.kafka.local-fallback.enabled:true}")
    private boolean localFallbackEnabled;

    @Scheduled(fixedDelayString = "${resipay.outbox.poll-interval-ms:2000}")
    @ConditionalOnProperty(name = "resipay.outbox.publisher.auto-enabled", havingValue = "true", matchIfMissing = false)
    public void scheduledPublish() {
        publishPendingEvents(50);
    }

    @Transactional
    public int publishPendingEvents(int batchSize) {
        List<OutboxEvent> pendingEvents = outboxRepository.findPendingEventsForUpdate(batchSize);
        if (pendingEvents.isEmpty()) {
            return 0;
        }

        boolean kafkaAvailable = kafkaAvailabilityService.isKafkaAvailable();
        log.info("Outbox publisher polling batch of {} pending events (Kafka broker: {})",
                pendingEvents.size(), kafkaAvailable ? "ONLINE" : "OFFLINE");

        int publishedCount = 0;

        for (OutboxEvent event : pendingEvents) {
            try {
                if (kafkaAvailable) {
                    // Publish using aggregateId as the partition key to guarantee chronological ordering
                    kafkaTemplate.send(
                            KafkaTopicConfig.PAYMENT_EVENTS_TOPIC,
                            event.getAggregateId(),
                            event.getPayload()
                    ).get(3, TimeUnit.SECONDS);

                    event.markPublished();
                    outboxRepository.save(event);
                    publishedCount++;
                    log.info("Published outbox event {} to Kafka for aggregate {}", event.getId(), event.getAggregateId());
                } else if (localFallbackEnabled) {
                    // Local fail-soft: dispatch in-memory to ledger handler so local dev works end-to-end without Kafka
                    dispatchLocalEvent(event);
                    event.markPublished();
                    outboxRepository.save(event);
                    publishedCount++;
                    log.info("Delivered outbox event {} via local fail-soft handler for aggregate {}",
                            event.getId(), event.getAggregateId());
                } else {
                    // Fail-soft: keep event PENDING without throwing or blocking
                    log.debug("Kafka offline and local fallback disabled. Retaining outbox event {} as PENDING", event.getId());
                }
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                event.markFailed(e.getMessage());
                outboxRepository.save(event);
            }
        }

        return publishedCount;
    }

    private void dispatchLocalEvent(OutboxEvent event) {
        try {
            JsonNode root = objectMapper.readTree(event.getPayload());
            paymentLedgerPostingHandler.handlePaymentEvent(root);
        } catch (Exception e) {
            log.warn("Local dispatch warning for outbox event {}: {}", event.getId(), e.getMessage());
        }
    }
}
