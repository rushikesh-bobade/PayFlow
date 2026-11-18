package com.payflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private String referenceId;
    private String type;
    private String status;
    private BigDecimal amount;
    private BigDecimal fee;
    private String sourceWalletNumber;
    private String targetWalletNumber;
    private String description;
    private String failureReason;
    private LocalDateTime createdAt;
}
