# Walking Skeleton — ResiPay

**Phase:** 1  
**Generated:** 2026-09-21  

## Capability Proven End-to-End

A client can submit a payment authorization request via REST API, which is persisted in PostgreSQL via versioned Flyway migrations, transitioned through a formal state machine guard, and processed against a deterministic provider simulator that reproduces explicit success, decline, and ambiguous network timeout outcomes (entering `UNKNOWN` state).

## Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Runtime & Language | Java 21 LTS | Project Loom virtual threads, records, pattern matching, type safety |
| Framework | Spring Boot 3.3.x | Enterprise transaction boundaries, REST APIs, Actuator metrics |
| Data Layer | PostgreSQL 16 + Flyway | ACID guarantees, schema evolution, check constraints on minor currency units |
| Precision Format | Integer Minor Units (`Long` cents) | Zero floating point math; eliminates rounding drift |
| State Management | Transition Matrix Validator | Explicit allowable state transitions rejecting illegal status mutations |
| Downstream Simulation | Deterministic In-Process Simulator | Header-driven failure injection (timeouts, 500, 429) and transaction audit store |

## Stack Touched in Phase 1

- [x] Project scaffold (`pom.xml`, Java 21 configuration, Spring Boot dependencies)
- [x] Database migrations (`src/main/resources/db/migration/V1__init_payments_schema.sql`)
- [x] Payment state machine (`PaymentStatus`, `PaymentStateMachine`, transition validation)
- [x] Payment core API (`POST /api/v1/payments`, `GET /api/v1/payments/{id}`)
- [x] Deterministic Provider Simulator (`POST /api/v1/simulator/charge`)
- [x] Automated test harness (JUnit 5, AssertJ, Mockito, Testcontainers PostgreSQL)

## Out of Scope (Deferred to Later Slices)

- Persistent idempotency key validation and response replay (Phase 2)
- Transactional outbox pattern and Kafka event publishing (Phase 3)
- Double-entry accounting ledger entries (`sum(debits) == sum(credits)`) (Phase 4)
- Context-aware retry controller, jitter, and circuit breaker (Phase 5)
- Automated reconciliation matching between internal and external logs (Phase 6)
- OpenTelemetry distributed tracing and Grafana dashboards (Phase 7)
- Baseline vs Proposed empirical comparative benchmark experiments (Phase 8)

## Subsequent Slice Plan

Each later phase adds one vertical slice on top of this skeleton without altering its architectural decisions:

- Phase 2: Persistent Idempotency Slice
- Phase 3: Transactional Outbox & Event-Driven Kafka Slice
- Phase 4: Immutable Double-Entry Ledger Slice
- Phase 5: Context-Aware Resilience & Retry Controller Slice
- Phase 6: Automated Reconciliation & Idempotent Webhook Slice
- Phase 7: End-to-End Observability, Security & Production Packaging
- Phase 8: Empirical Distributed Systems Research & Comparative Benchmarks
