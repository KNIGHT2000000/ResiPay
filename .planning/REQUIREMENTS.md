# Requirements: ResiPay Fintech Infrastructure

**Defined:** 2026-09-21  
**Core Value:** Preserving absolute financial correctness (zero duplicate financial effects, balanced double-entry ledger invariants) while safely navigating ambiguous distributed-system failures through idempotency, explicit state transitions, controlled retries, and automated reconciliation.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Core Payment State Machine & Invariants (STATE)

- [x] **STATE-01**: Payment entity supporting explicit states (CREATED, VALIDATED, PROCESSING, AUTHORIZED, COMPLETED, FAILED, DECLINED, UNKNOWN, CANCELLED, REFUND_PENDING, REFUNDED)
- [x] **STATE-02**: Formal state transition guard rejecting invalid status transitions (e.g., FAILED -> COMPLETED or COMPLETED -> PROCESSING)
- [x] **STATE-03**: First-class UNKNOWN state handling for ambiguous timeouts where downstream provider outcome is unconfirmed

### Persistent Idempotency (IDEM)

- [ ] **IDEM-01**: Persistent idempotency key validation ensuring same key + identical payload returns cached response
- [ ] **IDEM-02**: Idempotency conflict detection rejecting same key + altered payload with HTTP 422 (Unprocessable Entity)
- [ ] **IDEM-03**: Thread-safe concurrent execution guard preventing duplicate charges under concurrent identical requests

### Double-Entry Financial Ledger (LEDG)

- [ ] **LEDG-01**: Immutable double-entry bookkeeping with accounts, journal entries, and balanced debits and credits
- [ ] **LEDG-02**: Invariant enforcement: `sum(debits) == sum(credits)` for every posted journal entry
- [ ] **LEDG-03**: Integer minor units (e.g. cents as Long) used exclusively across all entities with zero floating-point representation
- [ ] **LEDG-04**: Compensating journal entries for reversals, chargebacks, and refunds with strictly immutable past postings

### Transactional Outbox & Event Streaming (OUTB)

- [ ] **OUTB-01**: Transactional outbox staging table committed atomically alongside payment and ledger mutations
- [ ] **OUTB-02**: Polling outbox relay with `SELECT FOR UPDATE SKIP LOCKED` publishing events reliably to Apache Kafka
- [ ] **OUTB-03**: Versioned event schemas (PaymentCreated, PaymentAuthorized, PaymentUnknown, etc.) with at-least-once delivery guarantees
- [ ] **OUTB-04**: Idempotent Kafka consumer event handler preventing duplicate processing upon redeliveries

### Deterministic Payment Provider Simulator (SIM)

- [x] **SIM-01**: Deterministic provider mock supporting configurable outcomes: SUCCESS, DECLINED, HTTP 500, HTTP 429, CONNECTION_RESET
- [x] **SIM-02**: Ambiguous failure simulation: TIMEOUT_BEFORE_PROCESSING vs TIMEOUT_AFTER_PROCESSING
- [x] **SIM-03**: Asynchronous callback simulation: DELAYED_RESPONSE, DUPLICATE_CALLBACK, OUT_OF_ORDER_CALLBACK
- [x] **SIM-04**: Provider internal transaction state store enabling ground-truth reconciliation verification

### Context-Aware Retry Controller & Resilience (RETR)

- [ ] **RETR-01**: Error classification engine distinguishing retryable, non-retryable, and unknown financial outcomes
- [ ] **RETR-02**: Exponential backoff with full jitter and execution deadline awareness
- [ ] **RETR-03**: Retry budget limiter restricting total retries to a bounded percentage of request volume
- [ ] **RETR-04**: Circuit breaker halting outbound requests during sustained provider degradation

### Automated Reconciliation Engine (RECON)

- [ ] **RECON-01**: Automated reconciliation job comparing internal transaction state against external provider transaction logs
- [ ] **RECON-02**: Multi-criteria matching engine supporting exact reference matching and composite deterministic matching
- [ ] **RECON-03**: Discrepancy taxonomy classification: MATCHED, DELAYED_MATCH, DUPLICATE, MISSING_INTERNAL, MISSING_EXTERNAL, AMOUNT_MISMATCH, STATUS_MISMATCH, AMBIGUOUS
- [ ] **RECON-04**: Escalation pipeline for AMBIGUOUS anomalies storing forensic evidence and audit trail

### Idempotent Webhook Ingestion (HOOK)

- [ ] **HOOK-01**: Webhook ingestion endpoint with signature verification and replay attack protection
- [ ] **HOOK-02**: Idempotent webhook processing handling duplicate deliveries without duplicate side effects
- [ ] **HOOK-03**: Out-of-order and delayed webhook handling preserving state machine invariants

### Observability & Security (OBSV)

- [ ] **OBSV-01**: OpenTelemetry distributed tracing propagating trace/span contexts across HTTP and Kafka boundaries
- [ ] **OBSV-02**: Prometheus metrics instrumenting payment throughput, retry volume, outbox lag, and circuit breaker state
- [ ] **OBSV-03**: Structured JSON logging with request IDs, correlation IDs, and payment IDs
- [ ] **OBSV-04**: Grafana dashboards for payment operations, resilience metrics, and reconciliation discrepancies
- [ ] **OBSV-05**: Spring Security RBAC enforcing roles (CUSTOMER, OPERATIONS, RECONCILIATION_ANALYST, ADMIN)

### Empirical Research Experiments & Benchmarks (EXPR)

- [ ] **EXPR-01**: Automated benchmark harness comparing Baseline (naïve retry) vs Proposed (context-aware retry + UNKNOWN + reconciliation)
- [ ] **EXPR-02**: Measurement and logging of retry amplification factor, P95/P99 latency, success rate, and duplicate financial effects
- [ ] **EXPR-03**: Synthetic reconciliation experiment evaluating matching accuracy and false positive rates under controlled anomalies
- [ ] **EXPR-04**: Python data analysis pipeline generating statistical summaries, distribution charts, and research report figures

### Production Infrastructure & Operations (OPS)

- [ ] **OPS-01**: Docker Compose environment orchestrating App, PostgreSQL, Kafka, Redis, Prometheus, and Grafana
- [x] **OPS-02**: Flyway version-controlled migration scripts for all database tables and constraints
- [ ] **OPS-03**: OpenAPI (Swagger) documentation for all client and operational endpoints
- [ ] **OPS-04**: Production-style documentation: Architecture Decision Records (ADRs), Threat Model, and Runbooks

## v2 Requirements

### Advanced Scaling & Platform Enhancements

- **SCAL-01**: Kubernetes deployment manifests (Deployments, Services, ConfigMaps, Secrets, HPA, Probes)
- **SCAL-02**: Priority-aware adaptive load shedding comparing critical payment traffic against background reporting
- **SCAL-03**: Change Data Capture (CDC) via Debezium replacing outbox polling

## Out of Scope

| Feature | Reason |
|---------|--------|
| Real Payment Provider Integration (Live Cards/Banks) | Security liability, financial risk, and non-reproducible for academic research |
| End-User Web UI / Checkout Front-End | Distracts from core backend distributed-systems and financial correctness focus |
| Microservice Network Sprawl | Modular monolith preserves ACID boundary and avoids unnecessary network hops |
| Floating-point currency math | Violates financial precision and causes ledger reconciliation failure |
| Synthetic data fabrication in research results | All benchmark figures must come from genuine executions of the experiment suite |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| STATE-01 | Phase 1 | Complete |
| STATE-02 | Phase 1 | Complete |
| STATE-03 | Phase 1 | Complete |
| SIM-01 | Phase 1 | Complete |
| SIM-02 | Phase 1 | Complete |
| SIM-03 | Phase 1 | Complete |
| SIM-04 | Phase 1 | Complete |
| OPS-02 | Phase 1 | Complete |
| IDEM-01 | Phase 2 | Pending |
| IDEM-02 | Phase 2 | Pending |
| IDEM-03 | Phase 2 | Pending |
| OUTB-01 | Phase 3 | Pending |
| OUTB-02 | Phase 3 | Pending |
| OUTB-03 | Phase 3 | Pending |
| OUTB-04 | Phase 3 | Pending |
| LEDG-01 | Phase 4 | Pending |
| LEDG-02 | Phase 4 | Pending |
| LEDG-03 | Phase 4 | Pending |
| LEDG-04 | Phase 4 | Pending |
| RETR-01 | Phase 5 | Pending |
| RETR-02 | Phase 5 | Pending |
| RETR-03 | Phase 5 | Pending |
| RETR-04 | Phase 5 | Pending |
| RECON-01 | Phase 6 | Pending |
| RECON-02 | Phase 6 | Pending |
| RECON-03 | Phase 6 | Pending |
| RECON-04 | Phase 6 | Pending |
| HOOK-01 | Phase 6 | Pending |
| HOOK-02 | Phase 6 | Pending |
| HOOK-03 | Phase 6 | Pending |
| OBSV-01 | Phase 7 | Pending |
| OBSV-02 | Phase 7 | Pending |
| OBSV-03 | Phase 7 | Pending |
| OBSV-04 | Phase 7 | Pending |
| OBSV-05 | Phase 7 | Pending |
| OPS-01 | Phase 7 | Pending |
| OPS-03 | Phase 7 | Pending |
| OPS-04 | Phase 7 | Pending |
| EXPR-01 | Phase 8 | Pending |
| EXPR-02 | Phase 8 | Pending |
| EXPR-03 | Phase 8 | Pending |
| EXPR-04 | Phase 8 | Pending |

**Coverage:**
- v1 requirements: 41 total
- Mapped to phases: 41
- Unmapped: 0 ✓

---
*Requirements defined: 2026-09-21*
*Last updated: 2026-09-21 after initial definition*
