package com.resipay.outbox.publisher;

import com.resipay.outbox.config.KafkaTopicConfig;
import com.resipay.outbox.domain.OutboxEvent;
import com.resipay.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        log.info("Outbox publisher polling batch of {} pending events", pendingEvents.size());
        int publishedCount = 0;

        for (OutboxEvent event : pendingEvents) {
            try {
                // Publish using aggregateId as the partition key to guarantee chronological ordering
                kafkaTemplate.send(
                        KafkaTopicConfig.PAYMENT_EVENTS_TOPIC,
                        event.getAggregateId(),
                        event.getPayload()
                ).get(5, TimeUnit.SECONDS);

                event.markPublished();
                outboxRepository.save(event);
                publishedCount++;
                log.info("Published outbox event {} for aggregate {}", event.getId(), event.getAggregateId());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                event.markFailed(e.getMessage());
                outboxRepository.save(event);
            }
        }

        return publishedCount;
    }
}
