package com.resipay.simulator.repository;

import com.resipay.simulator.domain.ProviderTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderTransactionRepository extends JpaRepository<ProviderTransaction, UUID> {

    Optional<ProviderTransaction> findByExternalReference(String externalReference);

    List<ProviderTransaction> findByPaymentId(UUID paymentId);

    List<ProviderTransaction> findByStatus(String status);
}
