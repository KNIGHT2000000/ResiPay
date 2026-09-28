package com.resipay.consumer.repository;

import com.resipay.consumer.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    boolean existsByEventIdAndConsumerGroup(UUID eventId, String consumerGroup);
}
