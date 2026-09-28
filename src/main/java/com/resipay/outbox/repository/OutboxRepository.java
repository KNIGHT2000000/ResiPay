package com.resipay.outbox.repository;

import com.resipay.outbox.domain.OutboxEvent;
import com.resipay.outbox.domain.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = "SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at ASC LIMIT :batchSize FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> findPendingEventsForUpdate(@Param("batchSize") int batchSize);

    List<OutboxEvent> findByAggregateId(String aggregateId);

    List<OutboxEvent> findByStatus(OutboxStatus status);
}
