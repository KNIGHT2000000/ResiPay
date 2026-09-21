# Roadmap: ResiPay Fintech Infrastructure

## Overview

ResiPay is built in 8 vertical MVP slices, moving methodically from core payment domain models and a deterministic provider simulator, through persistent idempotency, transactional outbox messaging, double-entry ledgering, and context-aware resilience, to automated reconciliation, production observability, and empirical research benchmarks. Every phase delivers a fully verified, runnable slice of functionality with rigorous automated tests.

## Phases

- [ ] **Phase 1: Payment Core Slice (Domain, State Machine & Deterministic Provider Simulator)** - Core payment lifecycle, state transition validator, Flyway migrations, and deterministic downstream mock with failure injection.
- [ ] **Phase 2: Persistent Idempotency Slice** - Persistent request deduplication, payload hashing, atomic locking, and response replay under high concurrency.
- [ ] **Phase 3: Transactional Outbox & Event-Driven Kafka Slice** - Dual-write-proof transactional outbox, polling publisher with SKIP LOCKED, versioned Kafka events, and idempotent consumers.
- [ ] **Phase 4: Immutable Double-Entry Ledger Slice** - Double-entry chart of accounts, immutable postings, integer minor unit arithmetic, invariant verification, and compensating reversals.
- [ ] **Phase 5: Context-Aware Resilience & Retry Controller Slice** - Error classification, exponential backoff with full jitter, retry budgets, deadlines, and circuit breaking.
- [ ] **Phase 6: Automated Reconciliation & Idempotent Webhook Slice** - Multi-criteria matching engine, discrepancy taxonomy, escalation pipeline, and idempotent webhook ingestion.
- [ ] **Phase 7: End-to-End Observability, Security & Production Packaging** - OpenTelemetry tracing, Prometheus metrics, Grafana dashboards, Spring Security RBAC, OpenAPI specs, and Docker Compose orchestration.
- [ ] **Phase 8: Empirical Distributed Systems Research & Comparative Benchmarks** - Automated benchmark harness comparing Baseline vs Proposed architectures, synthetic reconciliation tests, and Python statistical analysis.

## Phase Details

### Phase 1: Payment Core Slice (Domain, State Machine & Deterministic Provider Simulator)
**Goal:** Deliver working payment initialization and authorization slice backed by PostgreSQL and a deterministic provider simulator with simulated network failure modes.  
**Mode:** mvp  
**Depends on:** Nothing (first phase)  
**Requirements:** STATE-01, STATE-02, STATE-03, SIM-01, SIM-02, SIM-03, SIM-04, OPS-02  
**Success Criteria** (what must be TRUE):
  1. Client can submit payment authorization requests transitioning through validated state machine states (`CREATED`, `PROCESSING`, `AUTHORIZED`, `COMPLETED`, `FAILED`, `DECLINED`).
  2. Invalid state transitions (e.g. `FAILED` -> `COMPLETED` or `COMPLETED` -> `PROCESSING`) are strictly rejected with state machine errors.
  3. Provider simulator deterministically simulates `SUCCESS`, `DECLINED`, HTTP 500, HTTP 429, connection resets, and timeouts before/after processing.
  4. Ambiguous provider timeouts transition the payment into first-class `UNKNOWN` status (never falsely treated as `FAILED`).
**Plans:** 3 plans

Plans:
- [ ] 01-01: Domain entities, Flyway database schema, and payment repository setup
- [ ] 01-02: Formal payment state machine and transition validator with UNKNOWN state handling
- [ ] 01-03: Deterministic payment provider simulator with configurable failure modes and state storage

### Phase 2: Persistent Idempotency Slice
**Goal:** Protect payment endpoints with durable, thread-safe idempotency preventing duplicate financial side effects.  
**Mode:** mvp  
**Depends on:** Phase 1  
**Requirements:** IDEM-01, IDEM-02, IDEM-03  
**Success Criteria** (what must be TRUE):
  1. Submitting repeated identical payment requests with the same idempotency key returns the original cached response without re-executing.
  2. Reusing an existing idempotency key with altered payload (different amount/currency) is rejected with HTTP 422.
  3. High-concurrency stress test confirms zero duplicate payment records or double side-effects under concurrent submissions.
**Plans:** 2 plans

Plans:
- [ ] 02-01: Idempotency filter, payload hashing (SHA-256), and PostgreSQL/Redis storage schema
- [ ] 02-02: Concurrent locking, response caching, and concurrency integration tests

### Phase 3: Transactional Outbox & Event-Driven Kafka Slice
**Goal:** Enable reliable, dual-write-proof asynchronous messaging using transactional outbox and Apache Kafka.  
**Mode:** mvp  
**Depends on:** Phase 2  
**Requirements:** OUTB-01, OUTB-02, OUTB-03, OUTB-04  
**Success Criteria** (what must be TRUE):
  1. Payment state transitions atomically insert an outbox event in PostgreSQL within the same ACID transaction.
  2. Background outbox relay publishes pending events to Kafka and updates publication status without loss or duplicate emissions.
  3. Kafka consumer processes events idempotently and survives simulated consumer crash/restarts.
**Plans:** 3 plans

Plans:
- [ ] 03-01: Outbox table schema, event serialization models, and atomic staging on payment transitions
- [ ] 03-02: Outbox background publisher with `SELECT FOR UPDATE SKIP LOCKED` and Kafka producer integration
- [ ] 03-03: Idempotent Kafka consumer harness with event deduplication and crash-recovery verification

### Phase 4: Immutable Double-Entry Ledger Slice
**Goal:** Guarantee financial balance and auditability through double-entry accounting and compensating entries.  
**Mode:** mvp  
**Depends on:** Phase 3  
**Requirements:** LEDG-01, LEDG-02, LEDG-03, LEDG-04  
**Success Criteria** (what must be TRUE):
  1. Every processed payment generates an immutable double-entry journal entry with balanced debits and credits (`sum(debits) == sum(credits)`).
  2. All amounts are processed strictly in integer minor units (zero floating-point math).
  3. Reversals and refunds are posted as compensating journal entries leaving previous postings untouched.
  4. Automated ledger invariant verification passes across all accounts under volume testing.
**Plans:** 3 plans

Plans:
- [ ] 04-01: Chart of accounts, journal entries, and ledger postings schema and domain models
- [ ] 04-02: Asynchronous ledger posting consumer tied to payment events with balance verification
- [ ] 04-03: Compensating transactions for refunds/reversals and continuous ledger integrity test suite

### Phase 5: Context-Aware Resilience & Retry Controller Slice
**Goal:** Mitigate retry storms and protect downstream providers using failure classification, jittered backoff, retry budgets, and circuit breakers.  
**Mode:** mvp  
**Depends on:** Phase 4  
**Requirements:** RETR-01, RETR-02, RETR-03, RETR-04  
**Success Criteria** (what must be TRUE):
  1. System classifies provider errors into retryable, non-retryable, and ambiguous/unknown outcomes.
  2. Retry attempts apply exponential backoff with full jitter within configured request deadlines.
  3. Retry budget limits total retry volume during high downstream failure rates to prevent retry storms.
  4. Circuit breaker trips open during sustained provider failures and fast-fails new requests until provider recovers.
**Plans:** 3 plans

Plans:
- [ ] 05-01: Error classification engine and exponential backoff with full jitter and deadline propagation
- [ ] 05-02: Token-bucket retry budget controller restricting retry amplification
- [ ] 05-03: Circuit breaker integration and provider degradation chaos tests

### Phase 6: Automated Reconciliation & Idempotent Webhook Slice
**Goal:** Continuously reconcile internal state against external provider logs and safely ingest asynchronous webhooks.  
**Mode:** mvp  
**Depends on:** Phase 5  
**Requirements:** RECON-01, RECON-02, RECON-03, RECON-04, HOOK-01, HOOK-02, HOOK-03  
**Success Criteria** (what must be TRUE):
  1. Webhook endpoint processes external provider callbacks idempotently and handles out-of-order or delayed deliveries safely.
  2. Reconciliation engine runs automated matching between internal transactions and external provider logs.
  3. Discrepancies are categorized across the taxonomy (MATCHED, DELAYED_MATCH, DUPLICATE, MISSING, MISMATCH, AMBIGUOUS).
  4. Ambiguous cases are routed to an escalation queue with forensic evidence and audit logs.
**Plans:** 3 plans

Plans:
- [ ] 06-01: Secure, idempotent webhook ingestion endpoint with signature verification and replay guard
- [ ] 06-02: Multi-criteria reconciliation matching engine (exact reference & composite matching)
- [ ] 06-03: Discrepancy classification matrix, escalation storage, and automated reconciliation tests

### Phase 7: End-to-End Observability, Security & Production Packaging
**Goal:** Provide production-grade distributed tracing, operational telemetry, RBAC, and containerized deployment.  
**Mode:** mvp  
**Depends on:** Phase 6  
**Requirements:** OBSV-01, OBSV-02, OBSV-03, OBSV-04, OBSV-05, OPS-01, OPS-03, OPS-04  
**Success Criteria** (what must be TRUE):
  1. OpenTelemetry distributed tracing propagates correlation and trace context across HTTP and Kafka boundaries.
  2. Prometheus scrapes metrics for payment throughput, latencies, retry budgets, and circuit breaker status.
  3. Grafana dashboards visualize payment health, error rates, and reconciliation discrepancies.
  4. Spring Security enforces role-based access control (CUSTOMER, OPERATIONS, RECONCILIATION_ANALYST, ADMIN).
  5. Complete stack boots reproducibly via Docker Compose (`docker compose up`).
**Plans:** 3 plans

Plans:
- [ ] 07-01: OpenTelemetry, Prometheus Micrometer metrics, and structured JSON logging with correlation IDs
- [ ] 07-02: Spring Security RBAC, OpenAPI / Swagger documentation, and API access auditing
- [ ] 07-03: Docker Compose configuration, Grafana dashboard definitions, and production runbooks

### Phase 8: Empirical Distributed Systems Research & Comparative Benchmarks
**Goal:** Experimentally evaluate the central research hypothesis comparing Baseline vs Proposed architectures with Python statistical analysis.  
**Mode:** mvp  
**Depends on:** Phase 7  
**Requirements:** EXPR-01, EXPR-02, EXPR-03, EXPR-04  
**Success Criteria** (what must be TRUE):
  1. Automated experiment runner executes identical workloads against Baseline (naïve retry) and Proposed (context-aware retry + UNKNOWN + reconciliation).
  2. Empirical measurements capture retry amplification factor, P95/P99 latency, success rate, and duplicate financial side effects.
  3. Synthetic reconciliation experiment benchmarks matching accuracy across thousands of injected discrepancies.
  4. Python analysis scripts generate publication-grade statistical charts and final research report findings.
**Plans:** 3 plans

Plans:
- [ ] 08-01: Dual-engine experiment harness (Baseline naive retry vs Proposed context-aware resilience)
- [ ] 08-02: Synthetic transaction reconciliation benchmark with controlled discrepancy generator
- [ ] 08-03: Python statistical analysis suite, latency/retry distribution charts, and research findings report

## Progress

**Execution Order:**
Phases execute in numeric order: 1 ➔ 2 ➔ 3 ➔ 4 ➔ 5 ➔ 6 ➔ 7 ➔ 8

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Payment Core Slice | 0/3 | Not started | - |
| 2. Persistent Idempotency Slice | 0/2 | Not started | - |
| 3. Transactional Outbox & Event-Driven Kafka Slice | 0/3 | Not started | - |
| 4. Immutable Double-Entry Ledger Slice | 0/3 | Not started | - |
| 5. Context-Aware Resilience & Retry Controller Slice | 0/3 | Not started | - |
| 6. Automated Reconciliation & Idempotent Webhook Slice | 0/3 | Not started | - |
| 7. End-to-End Observability, Security & Production Packaging | 0/3 | Not started | - |
| 8. Empirical Distributed Systems Research & Comparative Benchmarks | 0/3 | Not started | - |
