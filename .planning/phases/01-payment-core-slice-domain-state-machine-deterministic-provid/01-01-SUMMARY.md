# Plan Summary: 01-01 Scaffold, Flyway Schema & Payment Entity

**Phase:** 01-payment-core-slice-domain-state-machine-deterministic-provid  
**Plan:** 01  
**Status:** Completed  

## Deliverables

1. **Root Maven Configuration (`pom.xml`)**:
   - Java 21 LTS runtime target with Spring Boot 3.3.3
   - PostgreSQL driver, Flyway 10.x migrations, Lombok, Spring Data JPA, Testcontainers 1.20.1
2. **Flyway Migration (`V1__init_payments_schema.sql`)**:
   - `payments` table with UUID primary key, `amount_cents` (`BIGINT`), ISO-4217 currency, optimistic locking version, and timestamp with time zone
   - Financial constraints: `CHECK (amount_cents > 0)` and `CHECK (status IN (...))`
   - `provider_transactions` table for deterministic simulator ground-truth audit trail
3. **Domain & Persistence**:
   - `PaymentStatus` enum with 11 formal states and terminal/ambiguous query helpers
   - `Payment` JPA entity with optimistic locking (`@Version`)
   - `PaymentRepository` extending `JpaRepository` with custom lookup methods
4. **Integration Test Suite**:
   - `AbstractPostgresIntegrationTest` supporting PostgreSQL Testcontainer and local PostgreSQL 18
   - `FlywayMigrationTest` verifying database constraints
   - `PaymentEntityTest` verifying lifecycle and optimistic locking

## Requirements Covered
- `STATE-01`: Payment entity with 11 explicit states
- `OPS-02`: Flyway version-controlled migration scripts for database tables and constraints
