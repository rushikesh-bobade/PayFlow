package com.payflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.dto.request.DepositRequest;
import com.payflow.dto.response.ApiResponse;
import com.payflow.dto.response.WalletResponse;
import com.payflow.entity.IdempotencyKey;
import com.payflow.entity.IdempotencyStatus;
import com.payflow.entity.User;
import com.payflow.service.IdempotencyService;
import com.payflow.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Tag(name = "Wallet Management", description = "Endpoints for checking wallet balance, auditing ledger alignment, and depositing funds")
public class WalletController {

    private final WalletService walletService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @GetMapping("/me")
    @Operation(summary = "Get authenticated user's wallet", description = "Retrieves current wallet details, balance, and real-time ledger audit status.")
    public ResponseEntity<ApiResponse<WalletResponse>> getMyWallet(@AuthenticationPrincipal User user) {
        WalletResponse response = walletService.getWalletByUser(user);
        return ResponseEntity.ok(ApiResponse.ok("Wallet retrieved successfully", response));
    }

    @GetMapping("/{walletNumber}")
    @Operation(summary = "Get wallet by wallet number", description = "Lookup wallet public details by wallet number.")
    public ResponseEntity<ApiResponse<WalletResponse>> getWalletByNumber(@PathVariable String walletNumber) {
        WalletResponse response = walletService.getWalletByNumber(walletNumber);
        return ResponseEntity.ok(ApiResponse.ok("Wallet details found", response));
    }

    @PostMapping("/{walletNumber}/deposit")
    @Operation(summary = "Deposit funds into wallet", description = "Deposits funds atomically and records a credit entry in the immutable ledger.")
    public ResponseEntity<ApiResponse<WalletResponse>> deposit(
            @PathVariable String walletNumber,
            @Valid @RequestBody DepositRequest request,
            @Parameter(description = "Unique UUID to guarantee idempotent request execution")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        // Idempotency check
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyKey> existing = idempotencyService.checkAndAcquire(
                    idempotencyKey,
                    "/api/v1/wallets/" + walletNumber + "/deposit",
                    request
            );
            if (existing.isPresent() && existing.get().getStatus() == IdempotencyStatus.COMPLETED) {
                try {
                    WalletResponse cached = objectMapper.readValue(existing.get().getResponsePayload(), WalletResponse.class);
                    return ResponseEntity.ok(ApiResponse.ok("Funds deposited successfully (idempotent cached replay)", cached));
                } catch (Exception ignored) {}
            }
        }

        try {
            WalletResponse response = walletService.deposit(walletNumber, request.getAmount(), request.getDescription());
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.markCompleted(idempotencyKey, response, 200);
            }
            return ResponseEntity.ok(ApiResponse.ok("Funds deposited successfully", response));
        } catch (Exception e) {
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.markFailed(idempotencyKey);
            }
            throw e;
        }
    }
}
