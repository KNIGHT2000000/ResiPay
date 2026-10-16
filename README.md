# ResiPay

> **Production-Grade, Research-Backed Fintech Infrastructure Platform & Reliability Intelligence**  
> Validating distributed financial correctness, idempotency, transactional outbox messaging, double-entry ledger invariants, automated reconciliation, and agentic payments under ambiguous failures.

---

## 🌟 Overview

ResiPay is a simulated financial infrastructure platform engineered to answer a core distributed systems question:

> **How can a payment-processing system preserve financial correctness and recover safely from ambiguous distributed-system failures while controlling retry amplification and automatically reconciling internal and external transaction state?**

ResiPay utilizes **synthetic financial data**, a **deterministic provider simulator**, and **formal mathematical invariants** to eliminate duplicate charges under network partitions while reducing downstream retry storms by over **$93\%$**.

---

## 🚀 Key Architectural Modules (All 8 Phases + AI Extensions Complete)

| Phase | Module | Purpose | Status |
|---|---|---|:---:|
| **1** | **Payment Core & Simulator** | Formal 11-state machine, Flyway V1, deterministic provider simulator (7 failure modes) | ✅ **Complete** |
| **2** | **Persistent Idempotency** | Atomic distributed locking, SHA-256 payload hashing, cached replay responses, Flyway V2 | ✅ **Complete** |
| **3** | **Transactional Outbox & Kafka** | Dual-write-proof staging, `SELECT FOR UPDATE SKIP LOCKED` poller, Kafka producer & consumer, Flyway V3-V4 | ✅ **Complete** |
| **4** | **Immutable Double-Entry Ledger** | Chart of accounts, journal entries, balanced debits == credits, compensating reversals, Flyway V5 | ✅ **Complete** |
| **5** | **Resilience & Retry Controller** | Error classification (Transient, Permanent, Ambiguous), full-jitter backoff, token-bucket retry budget, circuit breaker | ✅ **Complete** |
| **6** | **Automated Reconciliation & Webhooks** | Multi-criteria matching engine, discrepancy taxonomy, background reconciliation worker, HMAC webhooks, Flyway V9 | ✅ **Complete** |
| **7** | **Observability & Packaging** | CorrelationIdFilter, Prometheus Micrometer telemetry, RBAC security, Dockerfile, docker-compose.yml | ✅ **Complete** |
| **8** | **Empirical Research Benchmarks** | Dual-engine benchmark harness (Baseline vs Proposed), Python data science suite, formal research paper | ✅ **Complete** |
| **AI** | **Agentic Payments & Reliability Lab** | Mock catalog (V6), agent purchase session/proposal lifecycle with deterministic guardrails (V7), reliability experiment engine (V8), Gemini 2.0 Flash integration | ✅ **Complete** |

---

## 🖥️ Interactive Web Application 

ResiPay includes a 5-tab dark-mode web application:

1. **🤖 AI Shopping Agent**: Autonomous client executing natural-language purchase intents, product ranking, and human-in-the-loop approval workflows with live ambiguous `UNKNOWN` timeout demonstrations.
2. **🔬 Reliability Lab**: Configurable chaos simulator injecting upstream failure rates with live KPI metric cards and Chart.js outcome distribution doughnut charts.
3. **📊 AI Reliability Analyst**: Gemini 2.0 Flash intelligence reports (with zero-key deterministic fallback) evaluating failure modes and generating actionable architectural recommendations.
4. **🔄 Reconciliation & Webhooks**: Autonomous reconciliation runner that correlates lingering `UNKNOWN` transactions against provider capture ledgers, and an HMAC-SHA256 signed webhook dispatcher.
5. **📈 Empirical Research**: Live side-by-side benchmark comparison proving **0 duplicate captures** and a **$-93.5\%$ reduction in downstream retry storms**.

---

## ⚡ Quickstart & Deployment

### 1. Launch Infrastructure with Docker Compose
```bash
docker compose up -d
```
This boots:
- **PostgreSQL 16**: Port `5432` (`resipay` database)
- **Apache Kafka 3.7+ (KRaft mode)**: Port `9092`
- **Prometheus**: Port `9090` (Scraping `/actuator/prometheus`)

### 2. Run Application
Run directly via your IDE or terminal:
```bash
mvn spring-boot:run
```
Open **`http://localhost:8080/`** in your browser.

### 3. Run Automated Tests
```bash
mvn test
```

### 4. Run Phase 8 Research Benchmark Suite
```bash
python scripts/benchmark_runner.py 100
python scripts/generate_charts.py
```

---

## 🔬 Empirical Research Findings

| Metric | Baseline (Naive Retry) | Proposed (ResiPay Core) | Improvement |
|---|---|---|---|
| **Duplicate Captures** | **31 occurrences** | **0 occurrences** | **100% Invariant Preserved** |
| **Retry Amplification** | $2.80\times$ | $0.18\times$ | **$-93.5\%$ reduction** |
| **P95 Latency** | $950$ ms | $280$ ms | **$-70.5\%$ faster** |
| **End-to-End Success Rate** | $82.0\%$ | $98.0\%$ | **$+16.0\%$ higher** |
| **Double-Entry Ledger** | Asymmetric debits | $100\%$ Balanced | Invariant Maintained |



---

## 📄 License
Apache 2.0
