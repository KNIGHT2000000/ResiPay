-- ==============================================================================
-- V3__init_outbox_schema.sql
-- ResiPay Core Engine: Transactional Outbox Schema
-- Compatible with PostgreSQL 16+ / PostgreSQL 18
-- ==============================================================================

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    published_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,

    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

-- Index optimized for SELECT FOR UPDATE SKIP LOCKED batch polling
CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at ASC);

-- Index for aggregate event traceability
CREATE INDEX idx_outbox_aggregate ON outbox_events (aggregate_type, aggregate_id);
