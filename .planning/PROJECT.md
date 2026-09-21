# ResiPay

## What This Is

ResiPay is a production-grade, research-backed fintech infrastructure platform and simulated financial backend. It is designed to investigate and experimentally validate how a payment-processing system can preserve financial correctness and recover safely from ambiguous distributed-system failures while controlling retry amplification and automatically reconciling internal and external transaction states.

## Core Value

Preserving absolute financial correctness (zero duplicate financial effects, balanced double-entry ledger invariants) while safely navigating ambiguous distributed-system failures through idempotency, explicit state transitions, controlled retries, and automated reconciliation.

## Business & Research Context

- **Audience**: Backend engineering, fintech infrastructure, distributed systems, and platform evaluation.
- **Domain**: High-throughput, resilient payment processing and reconciliation infrastructure.
- **Central Hypothesis**: Context-aware retry and recovery control can reduce retry amplification and improve recovery behavior during dependency failures compared with naïve retry strategies, while maintaining payment correctness through idempotency, explicit state transitions, and reconciliation.
- **Data Invariant**: Synthetic financial references only; strictly zero real cards, bank credentials, or actual money.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] Comprehensive literature and industry research documented in `docs/research/industry-research.md` (Stripe, Uber, JPMorgan, Capital One, Adyen, AWS, GCP, and papers).
- [ ] Formal payment state machine with verified transitions and an explicit `UNKNOWN` state for ambiguous provider timeouts.
- [ ] Persistent, thread-safe idempotency subsystem preventing duplicate requests and side effects under high concurrency and node restarts.
- [ ] Immutable double-entry financial ledger enforcing `sum(debits) == sum(credits)` using integer minor units and compensating entries.
- [ ] Transactional outbox pattern guaranteeing atomic local database commits and reliable Apache Kafka event publication.
- [ ] Deterministic payment provider simulator supporting configurable failure modes (timeouts before/after processing, 429s, 500s, delayed/duplicate callbacks).
- [ ] Context-aware retry controller with failure classification, exponential backoff with jitter, retry budgets, execution deadlines, and circuit breaking.
- [ ] Automated reconciliation engine classifying discrepancies between internal state and external provider logs (MATCHED, DELAYED_MATCH, DUPLICATE, MISSING, MISMATCH, AMBIGUOUS).
- [ ] Secure, idempotent webhook ingestion engine with replay protection and support for delayed or out-of-order delivery.
- [ ] Reproducible failure injection framework across network transport, external providers, and asynchronous Kafka consumers.
- [ ] End-to-end observability stack with OpenTelemetry distributed tracing, Prometheus metrics, structured JSON logging, correlation IDs, and Grafana dashboards.
- [ ] Robust test harness: JUnit 5, Mockito, Testcontainers (PostgreSQL, Kafka, Redis), WireMock, concurrency stress tests, and ledger invariant verifiers.
- [ ] Central empirical experiment comparing Baseline (naïve retry) vs Proposed (context-aware retry + UNKNOWN state + reconciliation) with Python statistical analysis.
- [ ] Secondary experiment evaluating reconciliation matching accuracy across synthetic discrepancies.
- [ ] Production-ready infrastructure and documentation: Docker Compose, Flyway migrations, OpenAPI specs, ADRs, security threat model, operational runbooks, CI workflows, and optional Kubernetes manifests.

### Out of Scope

- Real banking networks or live payment credentials — synthetic references and simulated providers only to ensure safe, reproducible research.
- End-user graphical payment UI / shopping cart storefronts — ResiPay is purely backend infrastructure, core payment APIs, and operational telemetry.
- Microservice fragmentation — A domain-separated modular monolith is used to prioritize system correctness and operational clarity over distributed network overhead.
- Floating-point currency math — strictly forbidden; minor units (cents/integers) are enforced across the entire domain.
- Fabricated or unmeasured experimental results — all benchmark findings and research conclusions must stem from reproducible test executions.

## Context

Payment systems operate in inherently unreliable distributed environments where network timeouts, provider degradation, and duplicate callbacks threaten transactional consistency. Traditional naïve retry loops exacerbate provider outages through retry storms and risk double-debiting accounts when timeouts occur after a provider has processed a charge. ResiPay addresses these vulnerabilities through research-backed patterns pioneered by major financial and platform engineering institutions (Stripe, Uber, Adyen), combining persistent idempotency, transactional outbox, explicit unknown-state handling, immutable double-entry ledgering, and continuous asynchronous reconciliation.

## Constraints

- **Tech Stack**: Java 21 LTS, Spring Boot 3.x, Maven, PostgreSQL 16+, Flyway, Apache Kafka, Redis, Testcontainers.
- **Data Precision**: All monetary values represented in minor currency units (e.g., USD cents as `Long`) or precision `BigDecimal`; zero floating-point math.
- **Messaging Semantics**: Assume at-least-once message delivery; all consumers must implement idempotent processing; no global exactly-once guarantees.
- **Architectural Style**: Modular monolith with decoupled domain modules (Payment API, Orchestrator, Ledger, Outbox, Reconciliation, Retry Controller, Provider Simulator).
- **Tooling**: Python (pandas, matplotlib/seaborn) used strictly for experiment orchestration, benchmark data processing, and statistical visualization.
- **Verification Rule**: Small, independently verifiable phases; research before architecture; architecture before substantial code.

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Modular Monolith Architecture | Minimizes operational overhead and latency while maintaining clean domain boundaries between API, ledger, outbox, and reconciliation. | ⏳ Pending |
| First-Class `UNKNOWN` Payment State | Prevents ambiguous timeout responses from being marked as FAILED, avoiding catastrophic double-charges on subsequent retries. | ⏳ Pending |
| Immutable Double-Entry Ledger | Enforces auditability and financial balance (`sum(debits) == sum(credits)`); state corrections occur only via compensating entries. | ⏳ Pending |
| Transactional Outbox Pattern | Solves dual-write inconsistencies between PostgreSQL database operations and Kafka message publishing. | ⏳ Pending |
| Two-Tier Comparative Experimentation | Validates the central research hypothesis by comparing a naïve baseline against the proposed context-aware resilience architecture. | ⏳ Pending |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? ➔ Move to Out of Scope with reason
2. Requirements validated? ➔ Move to Validated with phase reference
3. New requirements emerged? ➔ Add to Active
4. Decisions to log? ➔ Add to Key Decisions
5. "What This Is" still accurate? ➔ Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-09-21 after initialization*
