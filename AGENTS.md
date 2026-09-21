<!-- GSD:project-start source:PROJECT.md -->

## Project

**ResiPay**

ResiPay is a production-grade, research-backed fintech infrastructure platform and simulated financial backend. It is designed to investigate and experimentally validate how a payment-processing system can preserve financial correctness and recover safely from ambiguous distributed-system failures while controlling retry amplification and automatically reconciling internal and external transaction states.

**Core Value:** Preserving absolute financial correctness (zero duplicate financial effects, balanced double-entry ledger invariants) while safely navigating ambiguous distributed-system failures through idempotency, explicit state transitions, controlled retries, and automated reconciliation.

### Constraints

- **Tech Stack**: Java 21 LTS, Spring Boot 3.x, Maven, PostgreSQL 16+, Flyway, Apache Kafka, Redis, Testcontainers.
- **Data Precision**: All monetary values represented in minor currency units (e.g., USD cents as `Long`) or precision `BigDecimal`; zero floating-point math.
- **Messaging Semantics**: Assume at-least-once message delivery; all consumers must implement idempotent processing; no global exactly-once guarantees.
- **Architectural Style**: Modular monolith with decoupled domain modules (Payment API, Orchestrator, Ledger, Outbox, Reconciliation, Retry Controller, Provider Simulator).
- **Tooling**: Python (pandas, matplotlib/seaborn) used strictly for experiment orchestration, benchmark data processing, and statistical visualization.
- **Verification Rule**: Small, independently verifiable phases; research before architecture; architecture before substantial code.

<!-- GSD:project-end -->

<!-- GSD:stack-start source:research/STACK.md -->

## Technology Stack

## Recommended Stack

### Core Framework & Language

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Java | 21 LTS | Core Runtime | Modern virtual threads (Project Loom), pattern matching, strong typing, mature ecosystem |
| Spring Boot | 3.3.x | Application Framework | Robust ecosystem for REST, transaction management, Actuator metrics, enterprise readiness |
| Spring Data JPA / JDBC | 3.3.x | Persistence Layer | JPA for entity management; native Spring JDBC queries for high-throughput idempotency/outbox operations |
| Spring Security | 6.x | Security & RBAC | Standardized authorization (CUSTOMER, OPERATIONS, RECONCILIATION_ANALYST, ADMIN) |
| Maven | 3.9+ | Build Tool | Predictable multi-module or modular monolith dependency management |

### Database & Persistence

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| PostgreSQL | 16+ | Primary Relational Store | Robust ACID transactions, `SELECT FOR UPDATE SKIP LOCKED` for outbox pollers, partial unique indices |
| Flyway | 10+ | Schema Migrations | Version-controlled, reproducible database evolution across environments |

### Messaging & Coordination

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Apache Kafka | 3.7+ | Event Broker | High-throughput asynchronous event streaming, partitioned ordering by payment ID, reliable consumer offsets |
| Redis | 7.x | Caching & Rate Limiting | Low-latency idempotency lock coordination, sliding-window rate limiting, token buckets |

### Testing & Verification

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| JUnit 5 & Mockito | 5.10+ | Unit Testing | Standard test harness and mock assertions |
| Testcontainers | 1.19+ | Integration Testing | Real PostgreSQL, Kafka, and Redis containers for integration testing |
| WireMock | 3.x | HTTP Mocking / Simulator | Precise HTTP simulation of external provider latencies, timeouts, and error codes |

### Observability & Infrastructure

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| OpenTelemetry | 1.35+ | Distributed Tracing | Vendor-neutral W3C tracecontext propagation across HTTP and Kafka |
| Prometheus | 2.50+ | Metrics Collection | Time-series metrics for retry rates, latency percentiles, outbox lag, circuit breaker status |
| Grafana | 10.x | Visualization | Operational dashboards for payments, reconciliation anomalies, and system health |
| Docker & Docker Compose | Latest | Local Orchestration | Single-command local environment (`docker compose up`) |

### Research & Data Analysis

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Python | 3.11+ | Experiment Harness & Analysis | Orchestrating benchmark runs, statistical analysis (pandas, scipy), chart generation (matplotlib/seaborn) |

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| Language | Java 21 LTS | Go / Rust | Java's enterprise Spring ecosystem has industry-standard transaction management and mature fintech patterns |
| Database | PostgreSQL | MongoDB / DynamoDB | Financial ledgers require strict ACID guarantees and transactional multi-table consistency (Ledger + Outbox) |
| Event Broker | Apache Kafka | RabbitMQ | Kafka provides persistent log replay, partition-ordered event streams, and durable consumer offset tracking |
| Migration Tool | Flyway | Liquibase | Flyway's plain SQL migrations provide direct visibility and simpler database-native tuning |

## Installation & Setup

# Core Java build

# Run integration tests with Testcontainers (requires Docker)

<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->

## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->

## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->

## Project Skills

No project skills found. Add skills to any of: `.agents/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->

## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:

- `/gsd-quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd-debug` for investigation and bug fixing
- `/gsd-execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->

## Developer Profile

> Profile not yet configured. Run `/gsd-profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
