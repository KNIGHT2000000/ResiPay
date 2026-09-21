package com.resipay.simulator;

import com.resipay.common.AbstractPostgresIntegrationTest;
import com.resipay.simulator.domain.ProviderTransaction;
import com.resipay.simulator.dto.SimulationOutcome;
import com.resipay.simulator.dto.SimulatorChargeRequest;
import com.resipay.simulator.repository.ProviderTransactionRepository;
import com.resipay.simulator.service.SimulatorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulatorTimeoutTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private SimulatorService simulatorService;

    @Autowired
    private ProviderTransactionRepository repository;

    @Test
    @DisplayName("SIM-02: TIMEOUT_BEFORE_PROCESSING times out without recording transaction in provider store")
    void shouldSimulateTimeoutBeforeProcessing() {
        UUID paymentId = UUID.randomUUID();
        SimulatorChargeRequest request = SimulatorChargeRequest.builder()
                .paymentId(paymentId)
                .amountCents(10000L)
                .currency("USD")
                .build();

        // 1. Invoke with TIMEOUT_BEFORE_PROCESSING (small latency for fast test execution)
        assertThatThrownBy(() -> simulatorService.processCharge(request, SimulationOutcome.TIMEOUT_BEFORE_PROCESSING, 50L))
                .isInstanceOf(ResponseStatusException.class);

        // 2. Verify NO record exists in provider store
        List<ProviderTransaction> records = repository.findByPaymentId(paymentId);
        assertThat(records).isEmpty();
    }

    @Test
    @DisplayName("SIM-02: TIMEOUT_AFTER_PROCESSING records transaction in provider store before timing out")
    void shouldSimulateTimeoutAfterProcessing() {
        UUID paymentId = UUID.randomUUID();
        SimulatorChargeRequest request = SimulatorChargeRequest.builder()
                .paymentId(paymentId)
                .amountCents(10000L)
                .currency("USD")
                .build();

        // 1. Invoke with TIMEOUT_AFTER_PROCESSING
        assertThatThrownBy(() -> simulatorService.processCharge(request, SimulationOutcome.TIMEOUT_AFTER_PROCESSING, 50L))
                .isInstanceOf(ResponseStatusException.class);

        // 2. Verify transaction WAS recorded in provider store as CAPTURED!
        List<ProviderTransaction> records = repository.findByPaymentId(paymentId);
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().getStatus()).isEqualTo("CAPTURED");
        assertThat(records.getFirst().getSimulatedOutcome()).isEqualTo("TIMEOUT_AFTER_PROCESSING");
    }
}
