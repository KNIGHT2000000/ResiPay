package com.resipay.simulator;

import com.resipay.simulator.dto.SimulationOutcome;
import com.resipay.simulator.dto.SimulatorChargeRequest;
import com.resipay.simulator.dto.SimulatorChargeResponse;
import com.resipay.simulator.repository.ProviderTransactionRepository;
import com.resipay.simulator.service.SimulatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProviderSimulatorTest {

    @Mock
    private ProviderTransactionRepository repository;

    private SimulatorService simulatorService;

    @BeforeEach
    void setUp() {
        simulatorService = new SimulatorService(repository);
    }

    @Test
    @DisplayName("SIM-01: SUCCESS outcome returns CAPTURED response and persists transaction")
    void shouldHandleSuccessOutcome() {
        SimulatorChargeRequest request = SimulatorChargeRequest.builder()
                .paymentId(UUID.randomUUID())
                .amountCents(7500L)
                .currency("USD")
                .build();

        SimulatorChargeResponse response = simulatorService.processCharge(request, SimulationOutcome.SUCCESS, null);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("CAPTURED");
        assertThat(response.externalReference()).startsWith("SIM-TXN-");
        verify(repository).save(any());
    }

    @Test
    @DisplayName("SIM-01: DECLINED outcome returns DECLINED response with reason")
    void shouldHandleDeclinedOutcome() {
        SimulatorChargeRequest request = SimulatorChargeRequest.builder()
                .paymentId(UUID.randomUUID())
                .amountCents(12000L)
                .currency("USD")
                .build();

        SimulatorChargeResponse response = simulatorService.processCharge(request, SimulationOutcome.DECLINED, null);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("DECLINED");
        assertThat(response.declineReason()).isEqualTo("INSUFFICIENT_FUNDS");
        verify(repository).save(any());
    }

    @Test
    @DisplayName("SIM-01: HTTP_500 outcome throws 500 Internal Server Error")
    void shouldThrowHttp500() {
        SimulatorChargeRequest request = SimulatorChargeRequest.builder()
                .paymentId(UUID.randomUUID())
                .amountCents(5000L)
                .currency("USD")
                .build();

        assertThatThrownBy(() -> simulatorService.processCharge(request, SimulationOutcome.HTTP_500, null))
                .isInstanceOf(ResponseStatusException.class)
                .matches(e -> ((ResponseStatusException) e).getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("SIM-01: HTTP_429 outcome throws 429 Too Many Requests")
    void shouldThrowHttp429() {
        SimulatorChargeRequest request = SimulatorChargeRequest.builder()
                .paymentId(UUID.randomUUID())
                .amountCents(5000L)
                .currency("USD")
                .build();

        assertThatThrownBy(() -> simulatorService.processCharge(request, SimulationOutcome.HTTP_429, null))
                .isInstanceOf(ResponseStatusException.class)
                .matches(e -> ((ResponseStatusException) e).getStatusCode() == HttpStatus.TOO_MANY_REQUESTS);
    }
}
