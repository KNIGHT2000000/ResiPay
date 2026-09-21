# Plan Summary: 01-03 Deterministic Payment Provider Simulator

**Phase:** 01-payment-core-slice-domain-state-machine-deterministic-provid  
**Plan:** 03  
**Status:** Completed  

## Deliverables

1. **Deterministic Simulation Outcomes (`SimulationOutcome.java`)**:
   - `SUCCESS`, `DECLINED`, `HTTP_500`, `HTTP_429`, `CONNECTION_RESET`, `TIMEOUT_BEFORE_PROCESSING`, `TIMEOUT_AFTER_PROCESSING`
2. **External Transaction Ground-Truth Store**:
   - `ProviderTransaction` entity mapped to `provider_transactions` table with external reference numbers and status
   - `ProviderTransactionRepository` providing lookup and audit query capabilities
3. **Simulator Service & Controller**:
   - `SimulatorService.processCharge(...)` honoring `X-Sim-Outcome` and `X-Sim-Latency-Ms` headers
   - `SimulatorController` at `POST /api/v1/simulator/charge` and `GET /api/v1/simulator/transactions/{paymentId}`
4. **Pre-Processing vs Post-Processing Timeout Simulation (SIM-02)**:
   - `TIMEOUT_BEFORE_PROCESSING`: times out without saving a provider record
   - `TIMEOUT_AFTER_PROCESSING`: persists a `CAPTURED` record in `provider_transactions` before sleeping/timing out, creating the ground-truth divergence required for reconciliation research
5. **Automated Verification Suite**:
   - `SimulatorStateStoreTest` confirming audit log persistence in PostgreSQL
   - `ProviderSimulatorTest` testing deterministic responses
   - `SimulatorTimeoutTest` proving state divergence under ambiguous timeouts

## Requirements Covered
- `SIM-01`: Deterministic provider mock supporting SUCCESS, DECLINED, 500, 429, CONNECTION_RESET
- `SIM-02`: Ambiguous failure simulation (TIMEOUT_BEFORE_PROCESSING vs TIMEOUT_AFTER_PROCESSING)
- `SIM-03`: Asynchronous callback simulation
- `SIM-04`: Provider internal transaction state store enabling ground-truth reconciliation verification
