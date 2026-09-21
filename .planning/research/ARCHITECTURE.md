# Architecture Patterns: ResiPay Fintech Platform

**Domain:** Payment Processing, Financial Ledgering, Distributed Resilience  
**Researched:** 2026-09-21  

## Recommended Architecture: Modular Monolith

ResiPay is architected as a domain-driven modular monolith deployed as a cohesive Spring Boot application, backed by PostgreSQL, Apache Kafka, and Redis.

```
       ┌────────────────────────────────────────────────────────┐
       │                   External Clients                     │
       └──────────────┬───────────────────────────┬─────────────┘
                      │ Payment REST API          │ Webhook Endpoint
                      ▼                           ▼
       ┌────────────────────────────────────────────────────────┐
       │                   ResiPay Core Engine                  │
       │                                                        │
       │   ┌─────────────────────┐    ┌─────────────────────┐   │
       │   │  Idempotency Guard  │    │  Webhook Processor  │   │
       │   └──────────┬──────────┘    └──────────┬──────────┘   │
       │              ▼                          ▼              │
       │   ┌────────────────────────────────────────────────┐   │
       │   │           Payment Orchestrator                 │   │
       │   │  (State Machine: CREATED → VALIDATED → ...)   │   │
       │   └───────┬─────────────────────────────┬──────────┘   │
       │           │                             │              │
       │           ▼                             ▼              │
       │   ┌────────────────┐            ┌──────────────────┐   │
       │   │ Retry / Circuit│            │  Transactional   │   │
       │   │   Controller   │            │      Outbox      │   │
       │   └───────┬────────┘            └────────┬─────────┘   │
       │           │                              │             │
       └───────────┼──────────────────────────────┼─────────────┘
                   │ HTTP                         │ Async
                   ▼                              ▼
       ┌─────────────────────┐          ┌───────────────────────┐
       │  Payment Provider   │          │     Apache Kafka      │
       │      Simulator      │          │ (payment & outbox msg)│
       │ (Failure Injection) │          └───────────┬───────────┘
       └───────────┬─────────┘                      │
                   │                                ▼
                   │                    ┌───────────────────────┐
                   │                    │  Double-Entry Ledger  │
                   │                    │  (Immutable Postings) │
                   │                    └───────────┬───────────┘
                   │ External                       │ Internal
                   │ Records                        │ Postings
                   ▼                                ▼
       ┌────────────────────────────────────────────────────────┐
       │                 Reconciliation Engine                  │
       │        (Automated Matching & Discrepancy Matrix)       │
       └────────────────────────────────────────────────────────┘
```

## Component Boundaries

| Component | Primary Responsibility | Data Store / Protocol |
|-----------|------------------------|-----------------------|
| **Payment API** | HTTP ingress, payload validation, authentication & RBAC | Spring MVC / REST |
| **Idempotency Subsystem** | Request fingerprinting, lock acquisition, response caching | PostgreSQL table / Redis |
| **Payment Orchestrator** | State transition validation, workflow coordination | PostgreSQL / Spring Data |
| **Retry Controller** | Context-aware error classification, jittered backoff, retry budget, circuit breaker | Resilience4j / Custom Engine |
| **Provider Simulator** | Deterministic upstream mock with configurable failure profiles | Embedded HTTP server / WireMock |
| **Transactional Outbox** | Transactional event staging and background publisher | PostgreSQL `outbox_events` -> Kafka |
| **Double-Entry Ledger** | Immutable accounts, journal entries, balanced postings | PostgreSQL `ledger_entries` |
| **Reconciliation Engine** | Discrepancy classification between internal & provider records | Batch / Stream consumer |
| **Observability Subsystem** | Metrics collection, distributed tracing, structured logging | Micrometer, OpenTelemetry, Prometheus |

## Architectural Invariants & Patterns

### 1. Dual-Write Elimination (Transactional Outbox)
Never perform a direct database write followed immediately by a network call to Kafka.
```sql
BEGIN TRANSACTION;
  -- 1. Update payment state to PROCESSING / AUTHORIZED
  UPDATE payments SET status = 'PROCESSING', updated_at = NOW() WHERE id = :id;
  -- 2. Stage outbox record in the same atomic transaction
  INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload, status)
  VALUES (gen_random_uuid(), 'PAYMENT', :id, 'PaymentProcessingStarted', :payload, 'PENDING');
COMMIT;
```

### 2. Double-Entry Balance Invariant
Every financial journal entry must balance debits and credits across integer minor units.
$$\sum \text{debits} = \sum \text{credits}$$
No direct updates or deletions of posted entries are permitted. Correcting a discrepancy requires an explicit compensating journal entry.

### 3. Safe Ambiguous State Handling
When a call to the provider simulator exceeds the network read timeout:
- The system must transition the payment to `UNKNOWN` (never `FAILED`).
- Active retries are halted until status verification or reconciliation confirms whether the provider processed the transaction.

## Scalability Considerations

| Concern | Baseline Scale (100 req/s) | High Scale (5,000 req/s) | Massive Overload (10,000+ req/s) |
|---------|---------------------------|--------------------------|----------------------------------|
| **Idempotency** | PostgreSQL unique constraint index | Redis distributed lock + PG persistence | Redis key reservation with TTL |
| **Outbox Relay** | Polling with `SKIP LOCKED` | Debezium CDC on PostgreSQL WAL | Partitioned worker pools |
| **Ledger Posting** | Synchronous inside worker thread | Asynchronous Kafka consumer batching | Sharded ledger accounts with buffer queues |
| **Provider Retries** | Synchronous backoff sleep | Asynchronous scheduled retry queues | Adaptive priority load shedding |
