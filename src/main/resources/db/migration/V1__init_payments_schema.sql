-- ==============================================================================
-- V1__init_payments_schema.sql
-- ResiPay Core Engine: Payments and Provider Simulator Audit Schema
-- Compatible with PostgreSQL 16+ / PostgreSQL 18
-- ==============================================================================

-- Enable UUID extension if not already present
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ------------------------------------------------------------------------------
-- Payments Table
-- Core lifecycle entity with immutable monetary check constraints
-- ------------------------------------------------------------------------------
CREATE TABLE payments (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255),
    customer_id VARCHAR(128) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    error_code VARCHAR(64),
    error_message TEXT,
    metadata JSONB,

    -- Financial correctness constraints
    CONSTRAINT chk_payments_amount_positive CHECK (amount_cents > 0),
    CONSTRAINT chk_payments_currency_iso CHECK (LENGTH(currency) = 3),
    CONSTRAINT chk_payments_status_valid CHECK (
        status IN (
            'CREATED',
            'VALIDATED',
            'PROCESSING',
            'AUTHORIZED',
            'COMPLETED',
            'FAILED',
            'DECLINED',
            'UNKNOWN',
            'CANCELLED',
            'REFUND_PENDING',
            'REFUNDED'
        )
    )
);

CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_customer_id ON payments (customer_id);
CREATE INDEX idx_payments_created_at ON payments (created_at DESC);

-- ------------------------------------------------------------------------------
-- Provider Transactions Table
-- Ground-truth audit store for simulated external payment provider
-- ------------------------------------------------------------------------------
CREATE TABLE provider_transactions (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    external_reference VARCHAR(64) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    simulated_outcome VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    response_payload JSONB,

    CONSTRAINT chk_provider_tx_amount_positive CHECK (amount_cents > 0),
    CONSTRAINT chk_provider_tx_status_valid CHECK (
        status IN ('PENDING', 'CAPTURED', 'DECLINED', 'VOIDED', 'REFUNDED')
    )
);

CREATE INDEX idx_provider_tx_payment_id ON provider_transactions (payment_id);
CREATE INDEX idx_provider_tx_external_ref ON provider_transactions (external_reference);
CREATE INDEX idx_provider_tx_created_at ON provider_transactions (created_at DESC);
