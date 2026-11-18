package com.payflow.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {

    @NotNull(message = "Deposit amount is required")
    @DecimalMin(value = "1.00", message = "Minimum deposit is $1.00")
    private BigDecimal amount;

    private String description;
}
