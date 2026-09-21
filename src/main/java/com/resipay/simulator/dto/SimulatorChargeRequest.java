package com.resipay.simulator.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulatorChargeRequest {

    @NotNull
    private UUID paymentId;

    @NotNull
    @Positive
    private Long amountCents;

    @NotNull
    private String currency;

    private String customerReference;
}
