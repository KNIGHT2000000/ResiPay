---
phase: "01"
slug: "payment-core-slice-domain-state-machine-deterministic-provid"
status: validated
nyquist_compliant: true
wave_0_complete: true
created: "2026-09-21"
---

# Phase 01 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito + Testcontainers PostgreSQL 16/18 |
| **Config file** | `pom.xml` |
| **Quick run command** | `mvn test -Dtest=*UnitTest` |
| **Full suite command** | `mvn test` |
| **Estimated runtime** | ~25 seconds |

---

## Sampling Rate

- **After every task commit:** Run `mvn test -Dtest=*UnitTest`
- **After every plan wave:** Run `mvn test`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 01-01-01 | 01 | 1 | OPS-02 | T-01-01 | Flyway rejects invalid schemas and enforces strict column constraints | integration | `mvn test -Dtest=FlywayMigrationTest` | ✅ | ✅ green |
| 01-01-02 | 01 | 1 | STATE-01 | T-01-02 | Monetary amounts validated > 0, currency ISO-4217 | unit | `mvn test -Dtest=PaymentEntityTest` | ✅ | ✅ green |
| 01-02-01 | 02 | 2 | STATE-02 | T-01-03 | Invalid state jumps rejected with IllegalStateException | unit | `mvn test -Dtest=PaymentStateMachineTest` | ✅ | ✅ green |
| 01-02-02 | 02 | 2 | STATE-03 | T-01-04 | Network timeouts transition payment to UNKNOWN, not FAILED | integration | `mvn test -Dtest=PaymentTimeoutIntegrationTest` | ✅ | ✅ green |
| 01-03-01 | 03 | 3 | SIM-01 | T-01-05 | Simulator validates simulation scenario headers | unit | `mvn test -Dtest=ProviderSimulatorTest` | ✅ | ✅ green |
| 01-03-02 | 03 | 3 | SIM-02 | T-01-06 | Simulator distinguishes pre vs post-commit timeouts | integration | `mvn test -Dtest=SimulatorTimeoutTest` | ✅ | ✅ green |
| 01-03-03 | 03 | 3 | SIM-03, SIM-04 | T-01-07 | Provider stores audit logs for reconciliation | integration | `mvn test -Dtest=SimulatorStateStoreTest` | ✅ | ✅ green |

*Status: ⏳ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `pom.xml` — Spring Boot 3.3.x, PostgreSQL driver, Flyway, Testcontainers JUnit Jupiter
- [x] `src/test/java/com/resipay/common/AbstractPostgresIntegrationTest.java` — Testcontainers setup
- [x] `src/main/resources/db/migration/V1__init_payments_schema.sql` — Initial database schema

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| None | All | N/A | All phase behaviors have automated verification via JUnit 5 and Testcontainers. |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-09-21
