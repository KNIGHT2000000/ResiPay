# Phase 1: Payment Core Slice (Domain, State Machine & Deterministic Provider Simulator) - Research

**Researched:** 2026-09-21  
**Domain:** Fintech Core Domain, State Machines, Flyway Migrations, Downstream Simulation  
**Confidence:** HIGH  

<user_constraints>
## User Constraints (from Initialization & ROADMAP.md)

### Locked Decisions
- **Runtime & Language**: Java 21 LTS (records, pattern matching, virtual threads) with Maven.
- **Framework**: Spring Boot 3.3.x (Spring Data JPA / Spring JDBC, Spring Web, Spring Validation).
- **Database**: PostgreSQL 16+ with Flyway schema migrations.
- **Monetary Representation**: Integer minor units (`Long` cents) or zero-scale `BigDecimal`; absolute zero floating-point math.
- **Architecture**: Modular monolith with decoupled domains (`payment`, `provider.simulator`).
- **Payment States**: Formal enum with `CREATED`, `VALIDATED`, `PROCESSING`, `AUTHORIZED`, `COMPLETED`, `FAILED`, `DECLINED`, `UNKNOWN`, `CANCELLED`, `REFUND_PENDING`, `REFUNDED`.
- **First-Class Ambiguity**: Ambiguous provider timeouts MUST transition to `UNKNOWN` state, never `FAILED`.
- **Provider Simulator**: Deterministic upstream simulator supporting configurable outcomes (`SUCCESS`, `DECLINED`, `HTTP_500`, `HTTP_429`, `CONNECTION_RESET`, `TIMEOUT_BEFORE_PROCESSING`, `TIMEOUT_AFTER_PROCESSING`).

### the agent's Discretion
- State machine implementation style (custom transition matrix validator vs Spring State Machine — custom transition matrix with clear transition tables is preferred for transparency and performance).
- Provider simulator protocol (embedded Spring RestController endpoint vs WireMock).
- Test harness structure using Testcontainers for PostgreSQL.

### Deferred Ideas (OUT OF SCOPE for Phase 1)
- Persistent idempotency cache & lock guard (Phase 2).
- Kafka publishing & outbox relay (Phase 3).
- Double-entry ledger postings (Phase 4).
- Dynamic circuit breakers and retry budgets (Phase 5).
- Automated reconciliation matching (Phase 6).
</user_constraints>

<architectural_responsibility_map>
## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Payment Entity & State Persistence | Database/Storage | API/Backend | Relational integrity in PostgreSQL via Flyway; JPA/JDBC mapping |
| State Transition Guard | API/Backend | Database/Storage | Enforces valid transitions and rejects invalid state mutations |
| Ambiguous Timeout Handling | API/Backend | - | Converts socket/read timeouts into non-terminal `UNKNOWN` state |
| Provider Simulator Engine | API/Backend | - | Deterministic HTTP endpoint mimicking upstream gateway with state history |
</architectural_responsibility_map>

## Summary

Phase 1 establishes the foundational backbone of ResiPay: the core domain entities, relational persistence with Flyway, the formal payment lifecycle state machine, and the deterministic payment provider simulator. 

Building a resilient payment infrastructure begins with modeling domain states with mathematical precision. In financial transactions, network failures during an outbound call to a provider represent an ambiguous outcome: the client does not know whether the upstream gateway processed the charge or dropped it. Phase 1 introduces a formal transition guard ensuring that ambiguous timeouts move to `UNKNOWN` instead of `FAILED`. Furthermore, Phase 1 delivers a deterministic payment provider simulator capable of reproducing timeouts before processing, timeouts after processing, HTTP 500s, 429s, and delayed responses.

## Standard Stack

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| Java | 21 LTS | Runtime | Virtual threads, records, pattern matching |
| Spring Boot | 3.3.3 | Core Framework | Enterprise-grade dependency injection, REST, transactions |
| PostgreSQL | 16+ | RDBMS | ACID transactions, constraint validation |
| Flyway | 10.x | Migrations | Version-controlled repeatable schema definition |
| Spring Data JPA / Hibernate | 6.5+ | ORM | Entity lifecycle, optimistic locking (`@Version`) |
| Testcontainers | 1.19+ | Testing | Real PostgreSQL container in JUnit 5 integration tests |
| WireMock / MockRestServiceServer | 3.x | Testing | HTTP boundary testing |

## Architecture Patterns

### 1. Finite State Machine via Transition Matrix
Rather than bloated switch statements or heavy third-party state machine frameworks, use an immutable transition matrix enum / lookup set:
```java
public enum PaymentStatus {
    CREATED, VALIDATED, PROCESSING, AUTHORIZED, COMPLETED, FAILED, DECLINED, UNKNOWN, CANCELLED, REFUND_PENDING, REFUNDED;

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = Map.of(
        CREATED, Set.of(VALIDATED, FAILED, CANCELLED),
        VALIDATED, Set.of(PROCESSING, FAILED, CANCELLED),
        PROCESSING, Set.of(AUTHORIZED, COMPLETED, DECLINED, FAILED, UNKNOWN),
        AUTHORIZED, Set.of(COMPLETED, FAILED, REFUND_PENDING, CANCELLED),
        UNKNOWN, Set.of(COMPLETED, FAILED, DECLINED, REFUNDED),
        COMPLETED, Set.of(REFUND_PENDING),
        REFUND_PENDING, Set.of(REFUNDED, COMPLETED)
        // Terminal states: FAILED, DECLINED, REFUNDED have no outgoing transitions
    );

    public boolean canTransitionTo(PaymentStatus next) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(next);
    }
}
```

### 2. Deterministic Provider Simulator Header Protocol
The client controls the simulator's failure profile via deterministic test scenario headers (e.g. `X-Sim-Outcome: TIMEOUT_AFTER_PROCESSING`, `X-Sim-Latency-Ms: 500`), while the simulator records its own transaction logs in a dedicated table or in-memory ledger for later reconciliation verification.

## Don't Hand-Roll

- **Database migrations**: Do not execute raw SQL manually or let Hibernate `ddl-auto=update` modify schemas. Use Flyway migrations (`V1__init_payments_schema.sql`).
- **Date/Time handling**: Do not use `java.util.Date` or `Calendar`. Use `Instant` (UTC) exclusively.
- **Currency handling**: Do not use `float` or `double`. Use `Long amountInCents` or `CurrencyUnit`.
- **Entity versioning**: Always add `@Version private Long version;` for optimistic locking to prevent lost updates under race conditions.

## Common Pitfalls

- **Pitfall**: Swallowing network read timeouts and defaulting to `PaymentStatus.FAILED`.
  - **Mitigation**: Catch `ResourceAccessException` / `SocketTimeoutException` in the payment client and explicitly transition status to `UNKNOWN`.
- **Pitfall**: Flyway checksum mismatch when editing existing migrations.
  - **Mitigation**: Treat migration scripts as strictly immutable once applied.
- **Pitfall**: Floating point rounding in test assertions or models.
  - **Mitigation**: Enforce minor units (`Long`) across all DTOs and database columns (`amount_cents BIGINT NOT NULL`).

## Validation Architecture

### Test Infrastructure
- **Test Framework**: JUnit 5 + AssertJ + Mockito + Testcontainers PostgreSQL
- **Quick run command**: `mvn test -Dtest=*UnitTest`
- **Full suite command**: `mvn test`
- **Estimated runtime**: ~15 seconds

### Automated Verification Map
- `STATE-01`: Payment entity schema and explicit state enumeration (`PaymentStateTest`)
- `STATE-02`: State transition matrix enforcement rejecting illegal transitions (`PaymentStateMachineTest`)
- `STATE-03`: Ambiguous timeout catch verifying payment enters `UNKNOWN` status (`PaymentTimeoutIntegrationTest`)
- `SIM-01`: Simulator returning configurable responses (SUCCESS, DECLINED, 500, 429) (`ProviderSimulatorTest`)
- `SIM-02`: Simulator simulating timeout before vs after internal commit (`SimulatorTimeoutTest`)
- `SIM-03`: Simulator asynchronous callback scenarios (`SimulatorCallbackTest`)
- `SIM-04`: Simulator storing internal external transaction state for reconciliation audit (`SimulatorStateStoreTest`)
- `OPS-02`: Flyway migrations applying cleanly on clean PostgreSQL (`FlywayMigrationTest`)

## Sources
- [Stripe: State Machines in Financial Workflows](https://stripe.com/blog)
- [Adyen: Handling Payment Timeouts](https://docs.adyen.com)
- [PostgreSQL Documentation: ACID & Check Constraints](https://www.postgresql.org/docs/)
- [Spring Boot 3.3 Reference: Data & RestClient](https://docs.spring.io/spring-boot/docs/current/reference/html/)
