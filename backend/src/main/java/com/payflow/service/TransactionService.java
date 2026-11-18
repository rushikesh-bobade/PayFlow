package com.payflow.service;

import com.payflow.dto.request.TransferRequest;
import com.payflow.dto.response.TransactionResponse;
import com.payflow.entity.*;
import com.payflow.exception.InsufficientBalanceException;
import com.payflow.exception.InvalidTransactionException;
import com.payflow.exception.ResourceNotFoundException;
import com.payflow.exception.WalletFrozenException;
import com.payflow.repository.TransactionRepository;
import com.payflow.repository.WalletRepository;
import com.payflow.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerService ledgerService;

    /**
     * Executes atomic money transfer between two wallets.
     * Uses pessimistic write locking with DETERMINISTIC LOCK ORDERING to prevent deadlocks and race conditions.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse transfer(String sourceWalletNumber, TransferRequest request) {
        if (sourceWalletNumber.equalsIgnoreCase(request.getTargetWalletNumber())) {
            throw new InvalidTransactionException("Cannot transfer funds to the same wallet");
        }

        BigDecimal transferAmount = MoneyUtils.formatStorage(request.getAmount());
        if (!MoneyUtils.isPositive(transferAmount)) {
            throw new InvalidTransactionException("Transfer amount must be strictly greater than zero");
        }

        // 1. Fetch wallet IDs first to determine deterministic lock order
        Wallet sourceMeta = walletRepository.findByWalletNumber(sourceWalletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Source wallet not found: " + sourceWalletNumber));

        Wallet targetMeta = walletRepository.findByWalletNumber(request.getTargetWalletNumber())
                .orElseThrow(() -> new ResourceNotFoundException("Target wallet not found: " + request.getTargetWalletNumber()));

        // 2. Deterministic Lock Ordering: Sort IDs to prevent database deadlocks
        Long firstLockId = sourceMeta.getId() < targetMeta.getId() ? sourceMeta.getId() : targetMeta.getId();
        Long secondLockId = sourceMeta.getId() < targetMeta.getId() ? targetMeta.getId() : sourceMeta.getId();

        log.debug("Acquiring pessimistic locks in order: ID {}, then ID {}", firstLockId, secondLockId);
        Wallet firstLocked = walletRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Failed to lock wallet with ID: " + firstLockId));
        Wallet secondLocked = walletRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Failed to lock wallet with ID: " + secondLockId));

        Wallet lockedSource = firstLocked.getId().equals(sourceMeta.getId()) ? firstLocked : secondLocked;
        Wallet lockedTarget = firstLocked.getId().equals(targetMeta.getId()) ? firstLocked : secondLocked;

        // 3. Status checks
        if (lockedSource.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletFrozenException("Source wallet is not active: " + sourceWalletNumber);
        }
        if (lockedTarget.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletFrozenException("Target wallet is not active: " + request.getTargetWalletNumber());
        }

        // 4. Concurrency-safe balance verification
        if (lockedSource.getBalance().compareTo(transferAmount) < 0) {
            throw new InsufficientBalanceException(String.format(
                    "Insufficient balance in wallet %s. Current: %s, Required: %s",
                    sourceWalletNumber,
                    MoneyUtils.formatDisplay(lockedSource.getBalance()),
                    MoneyUtils.formatDisplay(transferAmount)
            ));
        }

        // 5. Atomic Debit & Credit Mutations
        lockedSource.setBalance(lockedSource.getBalance().subtract(transferAmount));
        lockedTarget.setBalance(lockedTarget.getBalance().add(transferAmount));

        walletRepository.save(lockedSource);
        walletRepository.save(lockedTarget);

        // 6. Record Transaction entity
        Transaction transaction = Transaction.builder()
                .referenceId("TXN-TRF-" + UUID.randomUUID())
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.COMPLETED)
                .amount(transferAmount)
                .fee(BigDecimal.ZERO)
                .sourceWallet(lockedSource)
                .targetWallet(lockedTarget)
                .description(request.getDescription() != null ? request.getDescription() : "P2P Wallet Transfer")
                .build();
        Transaction savedTxn = transactionRepository.save(transaction);

        // 7. Double-Entry Accounting: Record immutable debit & credit ledger entries
        ledgerService.recordDebit(
                savedTxn,
                lockedSource,
                transferAmount,
                "Transfer out to " + lockedTarget.getWalletNumber()
        );
        ledgerService.recordCredit(
                savedTxn,
                lockedTarget,
                transferAmount,
                "Transfer in from " + lockedSource.getWalletNumber()
        );

        log.info("Transfer completed successfully: Ref {}, From {} To {}, Amount {}",
                savedTxn.getReferenceId(), sourceWalletNumber, lockedTarget.getWalletNumber(), transferAmount);

        return mapToResponse(savedTxn);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionsByWallet(String walletNumber, Pageable pageable) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + walletNumber));
        return transactionRepository.findAllByWallet(wallet, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByReference(String referenceId) {
        Transaction transaction = transactionRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + referenceId));
        return mapToResponse(transaction);
    }

    public TransactionResponse mapToResponse(Transaction txn) {
        return TransactionResponse.builder()
                .referenceId(txn.getReferenceId())
                .type(txn.getType().name())
                .status(txn.getStatus().name())
                .amount(MoneyUtils.formatDisplay(txn.getAmount()))
                .fee(MoneyUtils.formatDisplay(txn.getFee()))
                .sourceWalletNumber(txn.getSourceWallet() != null ? txn.getSourceWallet().getWalletNumber() : null)
                .targetWalletNumber(txn.getTargetWallet() != null ? txn.getTargetWallet().getWalletNumber() : null)
                .description(txn.getDescription())
                .failureReason(txn.getFailureReason())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
