package com.resipay.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resipay.outbox.domain.OutboxEvent;
import com.resipay.outbox.domain.OutboxStatus;
import com.resipay.outbox.event.PaymentEvent;
import com.resipay.outbox.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxStagingService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent stageEvent(String aggregateType, UUID aggregateId, PaymentEvent event) {
        try {
            String payloadJson = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(event.getEventId() != null ? event.getEventId() : UUID.randomUUID())
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId.toString())
                    .eventType(event.getEventType())
                    .payload(payloadJson)
                    .status(OutboxStatus.PENDING)
                    .retryCount(0)
                    .createdAt(Instant.now())
                    .build();

            log.info("Staging outbox event {} for {}/{}", outboxEvent.getId(), aggregateType, aggregateId);
            return outboxRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize outbox event payload", e);
            throw new RuntimeException("Outbox serialization failure", e);
        }
    }
}
