# Plan Summary: 01-02 Payment State Machine & UNKNOWN State Handling

**Phase:** 01-payment-core-slice-domain-state-machine-deterministic-provid  
**Plan:** 02  
**Status:** Completed  

## Deliverables

1. **Formal State Transition Machine (`PaymentStateMachine.java`)**:
   - Immutable transition map defining allowable source and target status pairs
   - Explicit terminal state protection (FAILED, DECLINED, REFUNDED reject all outgoing transitions)
   - Resolution pathways out of UNKNOWN to terminal states
   - `InvalidStateTransitionException` mapped to HTTP 409 Conflict
2. **Payment Application Layer**:
   - `CreatePaymentRequest` DTO with strict JSR-380 positive amount and 3-letter currency validation
   - `PaymentResponse` record exposing version and error details
   - `PaymentService` handling state transitions and persistence
   - `PaymentController` exposing REST endpoints (`POST /api/v1/payments`, `GET /api/v1/payments/{id}`)
3. **Ambiguous Timeout Handling (STATE-03)**:
   - `handleDownstreamTimeout` method explicitly mapping socket/read timeouts to `PaymentStatus.UNKNOWN`
   - Diagnostic error tracking: `DOWNSTREAM_TIMEOUT_AMBIGUOUS`
   - Integration test proving timeouts never default to `FAILED`
4. **Automated Test Suite**:
   - `PaymentStateMachineTest` with parameterized coverage of all valid and invalid transitions
   - `PaymentServiceTest` unit tests with Mockito
   - `PaymentTimeoutIntegrationTest` running on PostgreSQL verifying UNKNOWN persistence

## Requirements Covered
- `STATE-02`: Formal state transition guard rejecting invalid status transitions
- `STATE-03`: First-class UNKNOWN state handling for ambiguous timeouts where downstream provider outcome is unconfirmed
