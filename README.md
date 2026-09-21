# ResiPay

> **Production-Grade, Research-Backed Fintech Infrastructure Platform**  
> Experimental distributed financial backend validating correctness, idempotency, state transitions, and resilience under ambiguous failures.

---

## Overview

ResiPay is a simulated financial infrastructure platform engineered to answer a core distributed systems question:

> **How can a payment-processing system preserve financial correctness and recover safely from ambiguous distributed-system failures while controlling retry amplification and automatically reconciling internal and external transaction state?**

ResiPay utilizes **synthetic financial data** and a **deterministic provider simulator** to rigorously investigate distributed failure recovery, double-entry accounting invariants, transactional outbox patterns, and error classification.

---

## Core Capabilities (Implemented in Phase 1)

1. **Formal Payment State Machine**
   - Governed by an immutable transition matrix ([PaymentStateMachine.java](src/main/java/com/resipay/payment/domain/PaymentStateMachine.java)).
   - Enforces 11 explicit states: `CREATED`, `VALIDATED`, `PROCESSING`, `AUTHORIZED`, `COMPLETED`, `FAILED`, `DECLINED`, `UNKNOWN`, `CANCELLED`, `REFUND_PENDING`, `REFUNDED`.
   - Rejects illegal state mutations (e.g., `FAILED` → `COMPLETED`) with HTTP 409 Conflict.

2. **First-Class `UNKNOWN` State Handling**
   - Network read/socket timeouts during provider calls transition the transaction into `UNKNOWN` rather than prematurely failing it.
   - Eliminates catastrophic double-captures caused by naive retry loops on timed-out requests.

3. **Deterministic Provider Simulator**
   - In-process HTTP simulation engine reproducing 7 upstream provider outcomes:
     - `SUCCESS`
     - `DECLINED`
     - `HTTP_500` (Internal Server Error)
     - `HTTP_429` (Rate Limited)
     - `CONNECTION_RESET`
     - `TIMEOUT_BEFORE_PROCESSING` (Times out before storing transaction)
     - `TIMEOUT_AFTER_PROCESSING` (Captures transaction externally, then sleeps past client deadline)
   - Independent ground-truth audit ledger (`provider_transactions`) for automated reconciliation.

4. **Flyway Relational Migrations & Constraints**
   - Schema versioning via Flyway migrations ([V1__init_payments_schema.sql](src/main/resources/db/migration/V1__init_payments_schema.sql)).
   - Strictly positive minor unit amounts (`amount_cents BIGINT CHECK (amount_cents > 0)`).
   - Absolute zero floating-point math for monetary values.

---

## Tech Stack

- **Language & Runtime**: Java 21 LTS (Virtual Threads, Records, Pattern Matching)
- **Framework**: Spring Boot 3.3.3 (Spring Data JPA, Spring Web, Validation, Actuator)
- **Database**: PostgreSQL 16+ / PostgreSQL 18
- **Schema Migrations**: Flyway 10.x
- **Build Tool**: Maven
- **Testing**: JUnit 5, AssertJ, Mockito, Testcontainers

---

## Getting Started (IntelliJ IDEA & Local PostgreSQL 18)

### Prerequisites
- **JDK 21** or later installed
- **PostgreSQL 18** running locally on port `5432`
- **IntelliJ IDEA**

### 1. Create Local Database
Connect to your local PostgreSQL instance and create the database:
```sql
CREATE DATABASE resipay;
```

### 2. Configure Database Credentials (Optional)
Defaults in `src/main/resources/application.yml` target:
- URL: `jdbc:postgresql://localhost:5432/resipay`
- Username: `postgres`
- Password: `postgres`

You can override these with environment variables if needed:
```bash
export SPRING_DATASOURCE_USERNAME=your_user
export SPRING_DATASOURCE_PASSWORD=your_password
```

### 3. Open in IntelliJ IDEA
1. Open IntelliJ IDEA and select **Open** -> Choose `d:\project_finance_!`.
2. IntelliJ will detect `pom.xml` and import dependencies automatically.
3. Open the **Maven** tool window in IntelliJ and run:
   - `test` to run the complete test suite.
   - Or run `ResiPayApplication.java` directly from the IDE to start the service on port `8080`.

---

## API Endpoints

### 1. Create Payment
```http
POST /api/v1/payments
Content-Type: application/json

{
  "customerId": "cust_12345",
  "amountCents": 10000,
  "currency": "USD",
  "idempotencyKey": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d"
}
```

### 2. Get Payment
```http
GET /api/v1/payments/{id}
```

### 3. Invoke Provider Simulator
```http
POST /api/v1/simulator/charge
Content-Type: application/json
X-Sim-Outcome: TIMEOUT_AFTER_PROCESSING
X-Sim-Latency-Ms: 1500

{
  "paymentId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "amountCents": 10000,
  "currency": "USD"
}
```

---

## Roadmap

- [x] **Phase 1: Payment Core Slice & Deterministic Provider Simulator**
- [ ] **Phase 2: Persistent Idempotency & Concurrency Guard**
- [ ] **Phase 3: Transactional Outbox & Event-Driven Kafka Streaming**
- [ ] **Phase 4: Immutable Double-Entry Financial Ledger**
- [ ] **Phase 5: Context-Aware Resilience & Retry Controller**
- [ ] **Phase 6: Automated Reconciliation & Webhook Ingestion**
- [ ] **Phase 7: Observability, Security & Production Packaging**
- [ ] **Phase 8: Empirical Research Experiments & Benchmarks**

---

## License
Apache 2.0
