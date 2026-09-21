package com.resipay.simulator;

import com.resipay.common.AbstractPostgresIntegrationTest;
import com.resipay.simulator.domain.ProviderTransaction;
import com.resipay.simulator.repository.ProviderTransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SimulatorStateStoreTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private ProviderTransactionRepository repository;

    @Test
    @DisplayName("SIM-04: ProviderTransaction records persist and query by paymentId")
    @Transactional
    void shouldPersistAndRetrieveProviderTransaction() {
        UUID paymentId = UUID.randomUUID();
        String extRef = "SIM-TXN-ABC12345";

        ProviderTransaction tx = ProviderTransaction.builder()
                .paymentId(paymentId)
                .externalReference(extRef)
                .amountCents(10000L)
                .currency("USD")
                .status("CAPTURED")
                .simulatedOutcome("SUCCESS")
                .build();

        repository.saveAndFlush(tx);

        List<ProviderTransaction> found = repository.findByPaymentId(paymentId);
        assertThat(found).hasSize(1);
        assertThat(found.getFirst().getExternalReference()).isEqualTo(extRef);
        assertThat(found.getFirst().getStatus()).isEqualTo("CAPTURED");
        assertThat(found.getFirst().getAmountCents()).isEqualTo(10000L);
    }
}
