package com.homegenie.paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletTopUpRequest {
    @NotNull
    @Positive
    private BigDecimal amount;

    private String description;
    private String referenceId;
}
