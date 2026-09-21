package com.resipay.simulator.api;

import com.resipay.simulator.domain.ProviderTransaction;
import com.resipay.simulator.dto.SimulationOutcome;
import com.resipay.simulator.dto.SimulatorChargeRequest;
import com.resipay.simulator.dto.SimulatorChargeResponse;
import com.resipay.simulator.repository.ProviderTransactionRepository;
import com.resipay.simulator.service.SimulatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/simulator")
@RequiredArgsConstructor
public class SimulatorController {

    private final SimulatorService simulatorService;
    private final ProviderTransactionRepository providerTransactionRepository;

    @PostMapping("/charge")
    public ResponseEntity<SimulatorChargeResponse> charge(
            @Valid @RequestBody SimulatorChargeRequest request,
            @RequestHeader(value = "X-Sim-Outcome", required = false) SimulationOutcome outcome,
            @RequestHeader(value = "X-Sim-Latency-Ms", required = false) Long latencyMs
    ) {
        SimulatorChargeResponse response = simulatorService.processCharge(request, outcome, latencyMs);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/{paymentId}")
    public ResponseEntity<List<ProviderTransaction>> getTransactions(@PathVariable UUID paymentId) {
        List<ProviderTransaction> transactions = providerTransactionRepository.findByPaymentId(paymentId);
        return ResponseEntity.ok(transactions);
    }
}
