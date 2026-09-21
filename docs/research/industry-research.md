# ResiPay: Industry Engineering Research Foundation

This document synthesizes primary engineering literature, technical blogs, architectural whitepapers, and distributed systems research that establish the design principles of **ResiPay**.

---

## 1. Stripe Engineering: Distributed Idempotency and Payment Workflows

- **Company / Source**: Stripe Engineering (`brandur.org` / Stripe Developer Documentation & Architecture Blog)
- **Publication Date**: 2017-02-22 (Updated 2022)
- **Problem**: In distributed financial APIs, network blips, TCP timeouts, or client retries can cause identical payment requests to hit backend servers multiple times. Without protection, this leads to double-charging customers.
- **Why It Matters**: Financial correctness requires strict "at-most-once" execution semantics for financial side effects, even when HTTP/transport layers operate under "at-least-once" retry loops.
- **Documented Solution**:
  - Client supplies an `Idempotency-Key` header with UUIDv4.
  - An atomic reservation mechanism in the database stores the key, request hash, status (`PROCESSING`, `RESOLVED`), and the full HTTP response body and status code.
  - If a duplicate request arrives while `PROCESSING`, it waits or returns HTTP 409 / in-progress response.
  - Once completed, identical subsequent requests bypass the payment execution pipeline and replay the cached response.
  - If a subsequent request reuses the key with a conflicting payload (e.g., different amount or currency), it is rejected immediately with HTTP 422/400 (Idempotency Conflict).
- **Limitations & Trade-offs**:
  - Response caching consumes substantial database/cache storage.
  - Locks held during processing can introduce thread contention under high concurrency.
  - Requires explicit TTL policies and cache eviction strategy.
- **What is Reproducible in ResiPay**:
  - Persistent `idempotency_records` table in PostgreSQL with atomic `INSERT ON CONFLICT` or row locks.
  - SHA-256 payload hashing to verify request consistency.
  - Cached response replay and concurrent request queuing/locking.
- **Source URL**: [Stripe: Designing robust and predictable APIs with idempotency](https://stripe.com/blog/idempotency) / [Brandur: Implementing Stripe-like Idempotency Keys in Postgres](https://brandur.org/idempotency-keys)

---

## 2. Adyen: Payment Lifecycle, Ambiguous Timeouts, and Asynchronous State

- **Company / Source**: Adyen Tech Blog / Payment Processing Architecture Docs
- **Publication Date**: 2020 (Updated 2023)
- **Problem**: When invoking an upstream card acquirer or banking network, a network timeout, gateway connection reset, or read timeout leaves the client in an ambiguous state: the downstream system does not know if the acquirer processed the charge, declined it, or never received it.
- **Why It Matters**: Marking an ambiguous timeout as `FAILED` and retrying naively causes double captures. Marking it as `SUCCESS` without confirmation causes revenue leakage if the charge never settled.
- **Documented Solution**:
  - Explicit multi-stage state machines with a dedicated `UNKNOWN` or `PENDING_INVESTIGATION` state.
  - Decoupling synchronous API acknowledgement from terminal transaction finalization.
  - Asynchronous status polling and reconciliation against acquirer batch reports to resolve `UNKNOWN` states.
  - Explicit reverse/void attempts if cancellation is guaranteed safe, or escalation to manual review if state remains ambiguous after maximum recovery windows.
- **Limitations & Trade-offs**:
  - Introduces eventual consistency latency to payment outcomes.
  - Requires robust customer communication patterns for deferred confirmations.
- **What is Reproducible in ResiPay**:
  - Formally defined payment state machine with non-terminal `UNKNOWN` state upon timeout.
  - Out-of-band asynchronous status verification and automated reconciliation jobs.
- **Source URL**: [Adyen: Handling payment timeouts and edge cases](https://docs.adyen.com/development-resources/payment-states/)

---

## 3. Uber Engineering: Reliable Asynchronous Event Streaming & Outbox Pattern

- **Company / Source**: Uber Engineering Blog
- **Publication Date**: 2021-06-03
- **Problem**: Payment services must update their local relational database (e.g., PostgreSQL) and emit notifications to event brokers (e.g., Apache Kafka). Naive dual-writes (e.g., DB commit followed by `kafkaProducer.send()`) fail when the application crashes between the two steps, leading to missing events or phantom events if the DB transaction rolls back.
- **Why It Matters**: Ledger updates, fraud scoring, analytics, and partner notifications fall out of sync with actual payment states, destroying data integrity.
- **Documented Solution**:
  - Transactional Outbox Pattern: Every payment state mutation writes an event record to an `outbox` table within the same ACID database transaction.
  - A dedicated outbox poller / change-data-capture (CDC) relay reads pending events, publishes them to Kafka with retry and exponential backoff, and marks them `PUBLISHED` upon broker ACK.
  - At-least-once delivery guarantee combined with idempotent consumers.
- **Limitations & Trade-offs**:
  - Polling overhead on the primary database or complexity of Debezium/WAL streaming.
  - Risk of outbox backlog if Kafka is unreachable or throttled.
- **What is Reproducible in ResiPay**:
  - `outbox_events` table written transactionally with payment status and ledger postings.
  - Scheduled worker or worker loop publishing to Kafka topics (`payment.events`, `ledger.events`) with broker delivery confirmations.
- **Source URL**: [Uber Engineering: Reliable Processing in a Streaming Architecture](https://www.uber.com/blog/reliable-reprocessing/)

---

## 4. JPMorganChase / Modern Treasury: Double-Entry Ledger Invariants in Modern Banking

- **Company / Source**: JPMorganChase Technology / Modern Treasury Engineering Guides
- **Publication Date**: 2021-10-18 (Modern Treasury Ledger Design)
- **Problem**: Traditional single-balance "update account set balance = balance + 10" designs lose audit history, suffer from race conditions, and make balance auditing virtually impossible when transactions fail midway.
- **Why It Matters**: Financial regulations and accounting standards demand that money can neither appear nor disappear; every movement must have an auditable origin and destination.
- **Documented Solution**:
  - Immutable double-entry bookkeeping: every transaction consists of at least one debit and one credit.
  - Fundamental invariant: `sum(debits) - sum(credits) == 0`.
  - Ledger entries are strictly append-only (no updates, no deletes).
  - Adjustments and cancellations are recorded via explicit reversing/compensating journal entries.
  - Integer minor units (e.g., cents, satoshis, pence) prevent IEEE-754 floating-point rounding errors.
- **Limitations & Trade-offs**:
  - High storage volume due to append-only growth.
  - Calculating current balance requires aggregating immutable journal entries or maintaining snapshot balances with strict optimistic/pessimistic locking.
- **What is Reproducible in ResiPay**:
  - `ledger_accounts`, `journal_entries`, and `ledger_postings` tables.
  - Minor currency units stored as `BIGINT`.
  - Automated ledger integrity test suite verifying `sum(debits) == sum(credits)` across all accounts and journal entries.
- **Source URL**: [Modern Treasury: Ledger Architecture Guide](https://www.moderntreasury.com/journal/what-is-a-ledger)

---

## 5. Capital One: Event-Driven Banking and Outbox Architecture

- **Company / Source**: Capital One Tech Blog
- **Publication Date**: 2020-04-14
- **Problem**: Processing large volumes of payment authorization and settlement events without creating cascading failures or blocking critical API threads.
- **Why It Matters**: Bank core availability requirements exceed 99.999%. Upstream outages must not cascade down to customer checkout flows.
- **Documented Solution**:
  - Asynchronous event-driven choreography decoupled by Kafka.
  - Strict event schema versioning using JSON Schema or Avro.
  - Dead Letter Queues (DLQ) for malformed payloads or unrecoverable poison pill events.
  - Idempotent consumer workers tracking processed event IDs in local storage before committing Kafka offsets.
- **Limitations & Trade-offs**:
  - Eventual consistency requires handling out-of-order event arrivals.
  - Distributed tracing is mandatory to follow transaction paths across asynchronous boundaries.
- **What is Reproducible in ResiPay**:
  - Versioned Kafka events (`PaymentCreated`, `PaymentAuthorized`, `ReconciliationMismatchDetected`).
  - Consumer offset management and idempotent event deduplication tables.
- **Source URL**: [Capital One: Journey to Event-Driven Architecture](https://www.capitalone.com/tech/software-engineering/)

---

## 6. AWS Architecture Blog: Exponential Backoff, Full Jitter, and Retry Budgets

- **Company / Source**: AWS Architecture (Marc Brooker)
- **Publication Date**: 2015-03-03
- **Problem**: When a downstream payment service or provider degrades, clients retrying with fixed intervals or naive exponential backoff synchronize their requests, producing massive, cyclical load spikes known as "retry storms" or "thundering herds".
- **Why It Matters**: Naive retries can turn a brief 200ms brownout into a sustained multi-hour catastrophic outage.
- **Documented Solution**:
  - Full Jitter: Sleep duration is chosen uniformly at random between 0 and $\min(\text{cap}, \text{base} \times 2^{\text{attempt}})$.
  - Retry budgets: Limiting total retries to a fixed percentage (e.g., 10%) of total traffic to prevent amplification when failure rates surge.
  - Circuit Breakers: Halting calls entirely once downstream error rates cross a predetermined threshold.
- **Limitations & Trade-offs**:
  - Increases P95/P99 latency for failed requests.
  - Tuning backoff factors and jitter ranges requires empirical calibration under simulated load.
- **What is Reproducible in ResiPay**:
  - Direct comparison experiment between Naive Retry vs Full Jitter + Retry Budget + Circuit Breaker.
  - Metrics tracking `payment_retry_total` and retry amplification factor under failure injection.
- **Source URL**: [AWS Architecture: Exponential Backoff And Jitter](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/)

---

## 7. Distributed Systems Foundations: Distributed Transactions vs Eventual Reconciliation

- **Company / Source**: Pat Helland (ACM / Salesforce / Amazon) - *"Life beyond Distributed Transactions: an Apostate's Opinion"*
- **Publication Date**: 2007 (Updated 2016)
- **Problem**: Two-Phase Commit (2PC) over distributed network boundaries does not scale, has high latency, and blocks indefinitely when a coordinator or participant crashes.
- **Why It Matters**: In real-world payment networks, you cannot run a distributed 2PC between your internal database, an acquirer (e.g., Visa, Mastercard), and partner bank APIs.
- **Documented Solution**:
  - Design around independent transactional islands (entities with local ACID guarantees).
  - Bridge islands using asynchronous messaging, at-least-once delivery, and idempotence.
  - Resolve cross-island discrepancies using compensating transactions and automated asynchronous reconciliation.
- **Limitations & Trade-offs**:
  - Replaces immediate consistency with eventual consistency.
  - System must model and expose intermediate states (`PROCESSING`, `UNKNOWN`) to clients.
- **What is Reproducible in ResiPay**:
  - Modular monolith with local PostgreSQL ACID transactions per domain entity.
  - Asynchronous event bus via Kafka.
  - Reconciliation engine acting as the eventual consistency arbiter between internal state and simulated external provider records.
- **Source URL**: [ACM Queue: Life beyond Distributed Transactions](https://queue.acm.org/detail.cfm?id=3025012)
