package com.payflow.controller;

import com.payflow.dto.response.ApiResponse;
import com.payflow.dto.response.LedgerEntryResponse;
import com.payflow.dto.response.WalletResponse;
import com.payflow.entity.User;
import com.payflow.entity.Wallet;
import com.payflow.exception.ResourceNotFoundException;
import com.payflow.repository.LedgerEntryRepository;
import com.payflow.repository.WalletRepository;
import com.payflow.service.LedgerService;
import com.payflow.service.WalletService;
import com.payflow.util.MoneyUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
@Tag(name = "Ledger & Compliance Audit", description = "Endpoints for immutable double-entry ledger inspection and real-time balance reconciliation")
public class LedgerController {

    private final LedgerEntryRepository ledgerEntryRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final LedgerService ledgerService;

    @GetMapping("/my-entries")
    @Operation(summary = "Get immutable ledger entries", description = "Retrieves double-entry debit and credit audit log for the authenticated wallet.")
    public ResponseEntity<ApiResponse<Page<LedgerEntryResponse>>> getMyLedgerEntries(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        WalletResponse wallet = walletService.getWalletByUser(user);
        Page<LedgerEntryResponse> entries = ledgerEntryRepository.findAllByWalletIdOrderByCreatedAtDesc(
                user.getWallet().getId(),
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        ).map(entry -> LedgerEntryResponse.builder()
                .entryId(entry.getEntryId())
                .transactionReference(entry.getTransaction().getReferenceId())
                .walletNumber(wallet.getWalletNumber())
                .type(entry.getType().name())
                .amount(MoneyUtils.formatDisplay(entry.getAmount()))
                .balanceAfter(MoneyUtils.formatDisplay(entry.getBalanceAfter()))
                .description(entry.getDescription())
                .createdAt(entry.getCreatedAt())
                .build());

        return ResponseEntity.ok(ApiResponse.ok("Ledger entries retrieved", entries));
    }

    @GetMapping("/audit")
    @Operation(summary = "Reconcile wallet balance with ledger entries", description = "Computes sum(Credits) - sum(Debits) and verifies equality with current wallet balance.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> auditWallet(@AuthenticationPrincipal User user) {
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + user.getEmail()));

        BigDecimal netLedger = ledgerEntryRepository.calculateLedgerNetBalance(wallet.getId());
        boolean isAudited = ledgerService.verifyLedgerIntegrity(wallet.getId(), wallet.getBalance());

        Map<String, Object> auditReport = new HashMap<>();
        auditReport.put("walletNumber", wallet.getWalletNumber());
        auditReport.put("currentBalance", MoneyUtils.formatDisplay(wallet.getBalance()));
        auditReport.put("calculatedLedgerSum", MoneyUtils.formatDisplay(netLedger));
        auditReport.put("discrepancy", MoneyUtils.formatDisplay(wallet.getBalance().subtract(netLedger).abs()));
        auditReport.put("ledgerIntegrityVerified", isAudited);

        return ResponseEntity.ok(ApiResponse.ok("Ledger audit report generated", auditReport));
    }
}
