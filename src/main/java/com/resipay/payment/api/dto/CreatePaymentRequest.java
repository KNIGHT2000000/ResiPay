package com.resipay.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotBlank(message = "Customer ID must not be blank")
    private String customerId;

    @NotNull(message = "Amount in cents must not be null")
    @Positive(message = "Amount in cents must be strictly positive")
    private Long amountCents;

    @NotBlank(message = "Currency code must not be blank")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO-4217 code")
    private String currency;

    private String idempotencyKey;
}
