package com.payflow.service;

import com.payflow.dto.response.WalletResponse;
import com.payflow.entity.*;
import com.payflow.exception.ResourceNotFoundException;
import com.payflow.exception.WalletFrozenException;
import com.payflow.repository.LedgerEntryRepository;
import com.payflow.repository.TransactionRepository;
import com.payflow.repository.WalletRepository;
import com.payflow.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final LedgerEntryRepository ledgerEntryRepository;

    @Transactional
    public Wallet createWalletForUser(User user) {
        String walletNumber = generateUniqueWalletNumber();
        Wallet wallet = Wallet.builder()
                .walletNumber(walletNumber)
                .user(user)
                .balance(MoneyUtils.formatStorage(BigDecimal.ZERO))
                .currency("USD")
                .status(WalletStatus.ACTIVE)
                .build();
        return walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletByUser(User user) {
        Wallet wallet = walletRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + user.getEmail()));
        return mapToResponse(wallet);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletByNumber(String walletNumber) {
        Wallet wallet = walletRepository.findByWalletNumber(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found with number: " + walletNumber));
        return mapToResponse(wallet);
    }

    @Transactional
    public WalletResponse deposit(String walletNumber, BigDecimal amount, String description) {
        if (!MoneyUtils.isPositive(amount)) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }

        // Acquire pessimistic write lock
        Wallet wallet = walletRepository.findByWalletNumberForUpdate(walletNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + walletNumber));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new WalletFrozenException("Cannot deposit to non-active wallet: " + walletNumber);
        }

        BigDecimal updatedBalance = wallet.getBalance().add(MoneyUtils.formatStorage(amount));
        wallet.setBalance(updatedBalance);
        walletRepository.save(wallet);

        // Record Deposit Transaction
        Transaction transaction = Transaction.builder()
                .referenceId("TXN-DEP-" + UUID.randomUUID())
                .type(TransactionType.DEPOSIT)
                .status(TransactionStatus.COMPLETED)
                .amount(MoneyUtils.formatStorage(amount))
                .fee(BigDecimal.ZERO)
                .targetWallet(wallet)
                .description(description != null ? description : "Funds deposit")
                .build();
        transactionRepository.save(transaction);

        // Record double-entry credit ledger
        ledgerService.recordCredit(transaction, wallet, amount, "Deposit to wallet: " + walletNumber);

        log.info("Deposit successful: {} to wallet {}. New balance: {}", amount, walletNumber, updatedBalance);
        return mapToResponse(wallet);
    }

    public WalletResponse mapToResponse(Wallet wallet) {
        BigDecimal netLedger = ledgerEntryRepository.calculateLedgerNetBalance(wallet.getId());
        boolean isAudited = MoneyUtils.formatStorage(netLedger)
                .compareTo(MoneyUtils.formatStorage(wallet.getBalance())) == 0;

        return WalletResponse.builder()
                .walletNumber(wallet.getWalletNumber())
                .ownerName(wallet.getUser().getFullName())
                .ownerEmail(wallet.getUser().getEmail())
                .balance(MoneyUtils.formatDisplay(wallet.getBalance()))
                .currency(wallet.getCurrency())
                .status(wallet.getStatus().name())
                .ledgerNetBalance(MoneyUtils.formatDisplay(netLedger))
                .ledgerAudited(isAudited)
                .build();
    }

    private String generateUniqueWalletNumber() {
        SecureRandom random = new SecureRandom();
        String number;
        do {
            long randomDigits = 1000000000L + (long) (random.nextDouble() * 9000000000L);
            number = "WLT" + randomDigits;
        } while (walletRepository.findByWalletNumber(number).isPresent());
        return number;
    }
}
