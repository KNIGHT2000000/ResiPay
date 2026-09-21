# Technology Stack: ResiPay

**Project:** ResiPay Fintech Infrastructure  
**Researched:** 2026-09-21  

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
```bash
# Core Java build
mvn clean install -DskipTests

# Run integration tests with Testcontainers (requires Docker)
mvn test
```
