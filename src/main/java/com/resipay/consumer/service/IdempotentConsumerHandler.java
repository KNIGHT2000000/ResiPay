package com.resipay.consumer.service;

import com.resipay.consumer.domain.ProcessedEvent;
import com.resipay.consumer.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotentConsumerHandler {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public boolean processIdempotently(
            UUID eventId,
            String eventType,
            String aggregateId,
            String consumerGroup,
            Runnable action
    ) {
        if (processedEventRepository.existsByEventIdAndConsumerGroup(eventId, consumerGroup)) {
            log.info("Duplicate event {} already processed by consumer group {}. Skipping duplicate execution.",
                    eventId, consumerGroup);
            return false;
        }

        log.info("Processing event {} ({}) for aggregate {} in consumer group {}",
                eventId, eventType, aggregateId, consumerGroup);

        if (action != null) {
            action.run();
        }

        ProcessedEvent record = ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateId(aggregateId)
                .consumerGroup(consumerGroup)
                .processedAt(Instant.now())
                .build();

        processedEventRepository.save(record);
        return true;
    }
}
