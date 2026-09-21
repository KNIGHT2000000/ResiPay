# Domain Pitfalls: ResiPay Fintech Platform

**Domain:** Fintech Infrastructure & Distributed Systems  
**Researched:** 2026-09-21  

## Critical Pitfalls (Causes Rewrites or Financial Corruption)

### Pitfall 1: Treating Network Timeouts as Failures
- **What goes wrong:** When an HTTP client experiences a socket timeout while invoking a payment gateway, it marks the transaction as `FAILED` and initiates an automatic retry.
- **Why it happens:** In distributed networks, a timeout only indicates the absence of an acknowledgement within the timeout threshold—the upstream provider may have successfully processed the charge and deducted funds.
- **Consequences:** The subsequent retry causes a duplicate charge, debits the customer twice, and corrupts the ledger.
- **Prevention:** Mark the transaction as `UNKNOWN`. Never retry until status query or reconciliation confirms the external disposition.

### Pitfall 2: Dual-Write Inconsistency Between DB and Kafka
- **What goes wrong:** Committing a payment state update in PostgreSQL and then calling `kafkaTemplate.send(...)` in Java application code.
- **Why it happens:** If the application process crashes, network partitions, or Kafka throws an exception after the DB commit, the event is permanently lost. If done in reverse, a Kafka event might emit for a DB transaction that subsequently rolls back.
- **Consequences:** The ledger never creates corresponding entries for the payment, or creates ghost entries for non-existent payments.
- **Prevention:** Use the Transactional Outbox pattern with atomic database transactions.

### Pitfall 3: Floating-Point Math for Currency Calculations
- **What goes wrong:** Using `float` or `double` to store or calculate monetary amounts (e.g., `0.1 + 0.2 = 0.30000000000000004`).
- **Why it happens:** Binary floating-point cannot accurately represent decimal fractions.
- **Consequences:** Incremental rounding errors accumulate, causing ledger balancing checks (`sum(debits) == sum(credits)`) to fail.
- **Prevention:** Store all amounts in integer minor units (e.g., cents as `Long` or `BigInteger`) or use calibrated `BigDecimal` with strict rounding modes.

### Pitfall 4: Naive Retry Storms (Amplification Attack)
- **What goes wrong:** During downstream provider degradation, every failed request is retried with static intervals or naive exponential backoff without jitter or retry budgets.
- **Why it happens:** Retrying clients synchronize into waves of traffic ("thundering herd"), multiplying request volume 3x–10x.
- **Consequences:** A momentary 10% provider slowdown causes complete downstream collapse and prevents recovery.
- **Prevention:** Full jitter backoff, strict per-client/system retry budgets (max 10% retry traffic), and fast-failing circuit breakers.

## Moderate Pitfalls

### Pitfall 5: Out-of-Order Webhook Delivery
- **What goes wrong:** An asynchronous `PAYMENT_AUTHORIZED` webhook is delayed and arrives after a later `PAYMENT_REFUNDED` webhook has already been processed.
- **Prevention:** Monotonic versioning or timestamp checks in the payment state machine to discard stale webhook updates.

### Pitfall 6: Non-Idempotent Event Consumers
- **What goes wrong:** A Kafka consumer crashes after writing to the ledger but before committing its Kafka offset, causing the event to be redelivered and processed a second time.
- **Prevention:** Consume events idempotently by recording processed event IDs in an atomic transaction table.

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| State Machine | Allowing invalid backwards transitions (e.g., `COMPLETED` -> `FAILED`) | Enforce formal transition matrix with strict state checks |
| Idempotency | Lock contention under burst traffic on the same key | Non-blocking `INSERT ... ON CONFLICT DO NOTHING` + row-level polling |
| Ledger | Mutating existing postings instead of appending adjustments | Database trigger or JPA entity restriction preventing `UPDATE` and `DELETE` on postings |
| Outbox Relay | Poller lock contention across multiple application instances | Use `SELECT FOR UPDATE SKIP LOCKED` |
| Reconciliation | Silently guessing the resolution of ambiguous mismatches | Escalate `AMBIGUOUS` records for review with audit trail |
