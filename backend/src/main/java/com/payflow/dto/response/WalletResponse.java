package com.payflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponse {
    private String walletNumber;
    private String ownerName;
    private String ownerEmail;
    private BigDecimal balance;
    private String currency;
    private String status;
    private BigDecimal ledgerNetBalance;
    private boolean ledgerAudited;
}
