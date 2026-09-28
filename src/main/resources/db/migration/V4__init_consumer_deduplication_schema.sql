-- ==============================================================================
-- V4__init_consumer_deduplication_schema.sql
-- ResiPay Core Engine: Consumer Event Deduplication Schema
-- Compatible with PostgreSQL 16+ / PostgreSQL 18
-- ==============================================================================

CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    consumer_group VARCHAR(64) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_processed_events_aggregate ON processed_events (aggregate_id);
