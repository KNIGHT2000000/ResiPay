package com.resipay.simulator.service;

import com.resipay.simulator.domain.ProviderTransaction;
import com.resipay.simulator.dto.SimulationOutcome;
import com.resipay.simulator.dto.SimulatorChargeRequest;
import com.resipay.simulator.dto.SimulatorChargeResponse;
import com.resipay.simulator.repository.ProviderTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulatorService {

    private final ProviderTransactionRepository providerTransactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = ResponseStatusException.class)
    public SimulatorChargeResponse processCharge(
            SimulatorChargeRequest request,
            SimulationOutcome outcome,
            Long latencyMs
    ) {
        SimulationOutcome resolvedOutcome = (outcome != null) ? outcome : SimulationOutcome.SUCCESS;
        String externalRef = "SIM-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("Simulator processing charge for paymentId: {}, amount: {} {}, outcome: {}, latencyMs: {}",
                request.getPaymentId(), request.getAmountCents(), request.getCurrency(), resolvedOutcome, latencyMs);

        // Optional simulated network latency
        applyLatency(latencyMs);

        switch (resolvedOutcome) {
            case SUCCESS -> {
                ProviderTransaction tx = ProviderTransaction.builder()
                        .paymentId(request.getPaymentId())
                        .externalReference(externalRef)
                        .amountCents(request.getAmountCents())
                        .currency(request.getCurrency())
                        .status("CAPTURED")
                        .simulatedOutcome(resolvedOutcome.name())
                        .createdAt(Instant.now())
                        .build();
                providerTransactionRepository.save(tx);
                return SimulatorChargeResponse.success(tx.getId(), request.getPaymentId(), externalRef, request.getAmountCents(), request.getCurrency());
            }

            case DECLINED -> {
                ProviderTransaction tx = ProviderTransaction.builder()
                        .paymentId(request.getPaymentId())
                        .externalReference(externalRef)
                        .amountCents(request.getAmountCents())
                        .currency(request.getCurrency())
                        .status("DECLINED")
                        .simulatedOutcome(resolvedOutcome.name())
                        .createdAt(Instant.now())
                        .build();
                providerTransactionRepository.save(tx);
                return SimulatorChargeResponse.declined(tx.getId(), request.getPaymentId(), externalRef, request.getAmountCents(), request.getCurrency(), "INSUFFICIENT_FUNDS");
            }

            case HTTP_500 -> throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Simulated upstream 500 internal server error");

            case HTTP_429 -> throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Simulated upstream 429 rate limit exceeded");

            case CONNECTION_RESET -> throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Simulated upstream connection reset by peer");

            case TIMEOUT_BEFORE_PROCESSING -> {
                log.warn("SIMULATOR: TIMEOUT_BEFORE_PROCESSING — Sleeping without recording transaction");
                applyLatency(latencyMs != null ? latencyMs : 2000L);
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Gateway timeout before processing");
            }

            case TIMEOUT_AFTER_PROCESSING -> {
                log.warn("SIMULATOR: TIMEOUT_AFTER_PROCESSING — Persisting CAPTURED transaction then sleeping past client deadline");
                ProviderTransaction tx = ProviderTransaction.builder()
                        .paymentId(request.getPaymentId())
                        .externalReference(externalRef)
                        .amountCents(request.getAmountCents())
                        .currency(request.getCurrency())
                        .status("CAPTURED")
                        .simulatedOutcome(resolvedOutcome.name())
                        .createdAt(Instant.now())
                        .build();
                providerTransactionRepository.saveAndFlush(tx);

                applyLatency(latencyMs != null ? latencyMs : 2000L);
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Gateway timeout after processing");
            }

            default -> throw new IllegalStateException("Unhandled simulation outcome: " + resolvedOutcome);
        }
    }

    private void applyLatency(Long latencyMs) {
        if (latencyMs != null && latencyMs > 0) {
            try {
                Thread.sleep(latencyMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Simulation delay interrupted", e);
            }
        }
    }
}
