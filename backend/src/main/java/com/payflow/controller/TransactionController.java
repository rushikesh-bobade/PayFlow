package com.payflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.dto.request.TransferRequest;
import com.payflow.dto.response.ApiResponse;
import com.payflow.dto.response.TransactionResponse;
import com.payflow.dto.response.WalletResponse;
import com.payflow.entity.IdempotencyKey;
import com.payflow.entity.IdempotencyStatus;
import com.payflow.entity.User;
import com.payflow.service.IdempotencyService;
import com.payflow.service.TransactionService;
import com.payflow.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions & Transfers", description = "Endpoints for executing atomic money transfers and querying transaction logs")
public class TransactionController {

    private final TransactionService transactionService;
    private final WalletService walletService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money to another wallet", description = "Executes an atomic transfer using pessimistic locking, deadlock prevention, and double-entry ledger posting.")
    public ResponseEntity<ApiResponse<TransactionResponse>> transfer(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TransferRequest request,
            @Parameter(description = "UUID key ensuring duplicate requests produce identical safe results")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        WalletResponse userWallet = walletService.getWalletByUser(user);
        String endpoint = "/api/v1/transactions/transfer";

        // 1. Idempotency Guard
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyKey> existing = idempotencyService.checkAndAcquire(idempotencyKey, endpoint, request);
            if (existing.isPresent() && existing.get().getStatus() == IdempotencyStatus.COMPLETED) {
                try {
                    TransactionResponse cached = objectMapper.readValue(
                            existing.get().getResponsePayload(),
                            TransactionResponse.class
                    );
                    return ResponseEntity.ok(ApiResponse.ok("Transfer completed (idempotent replay)", cached));
                } catch (Exception ignored) {}
            }
        }

        // 2. Execute Transfer Workflow
        try {
            TransactionResponse response = transactionService.transfer(userWallet.getWalletNumber(), request);

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.markCompleted(idempotencyKey, response, HttpStatus.OK.value());
            }

            return ResponseEntity.ok(ApiResponse.ok("Transfer executed successfully", response));
        } catch (Exception ex) {
            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                idempotencyService.markFailed(idempotencyKey);
            }
            throw ex;
        }
    }

    @GetMapping("/my-transactions")
    @Operation(summary = "Get user transaction history", description = "Retrieves paginated list of debits and credits for authenticated user's wallet.")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getMyTransactions(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        WalletResponse userWallet = walletService.getWalletByUser(user);
        Page<TransactionResponse> transactions = transactionService.getTransactionsByWallet(
                userWallet.getWalletNumber(),
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return ResponseEntity.ok(ApiResponse.ok("Transactions retrieved", transactions));
    }

    @GetMapping("/{referenceId}")
    @Operation(summary = "Get transaction details by reference ID", description = "Query specific transaction status and metadata.")
    public ResponseEntity<ApiResponse<TransactionResponse>> getByReference(@PathVariable String referenceId) {
        TransactionResponse response = transactionService.getTransactionByReference(referenceId);
        return ResponseEntity.ok(ApiResponse.ok("Transaction found", response));
    }
}
