package com.payflow.service;

import com.payflow.entity.*;
import com.payflow.repository.LedgerEntryRepository;
import com.payflow.util.MoneyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    /**
     * Appends an immutable debit ledger entry for a wallet.
     */
    @Transactional
    public LedgerEntry recordDebit(Transaction transaction, Wallet wallet, BigDecimal amount, String description) {
        LedgerEntry entry = LedgerEntry.builder()
                .entryId("LED-DR-" + UUID.randomUUID())
                .transaction(transaction)
                .wallet(wallet)
                .type(LedgerEntryType.DEBIT)
                .amount(MoneyUtils.formatStorage(amount))
                .balanceAfter(MoneyUtils.formatStorage(wallet.getBalance()))
                .description(description)
                .build();
        return ledgerEntryRepository.save(entry);
    }

    /**
     * Appends an immutable credit ledger entry for a wallet.
     */
    @Transactional
    public LedgerEntry recordCredit(Transaction transaction, Wallet wallet, BigDecimal amount, String description) {
        LedgerEntry entry = LedgerEntry.builder()
                .entryId("LED-CR-" + UUID.randomUUID())
                .transaction(transaction)
                .wallet(wallet)
                .type(LedgerEntryType.CREDIT)
                .amount(MoneyUtils.formatStorage(amount))
                .balanceAfter(MoneyUtils.formatStorage(wallet.getBalance()))
                .description(description)
                .build();
        return ledgerEntryRepository.save(entry);
    }

    /**
     * Verifies financial integrity: Wallet balance must equal sum(Credit) - sum(Debit).
     */
    @Transactional(readOnly = true)
    public boolean verifyLedgerIntegrity(Long walletId, BigDecimal currentBalance) {
        BigDecimal netLedgerBalance = ledgerEntryRepository.calculateLedgerNetBalance(walletId);
        boolean matches = MoneyUtils.formatStorage(netLedgerBalance)
                .compareTo(MoneyUtils.formatStorage(currentBalance)) == 0;

        if (!matches) {
            log.error("LEDGER INTEGRITY BREACH! Wallet ID: {}, Cached Balance: {}, Ledger Sum: {}",
                    walletId, currentBalance, netLedgerBalance);
        }
        return matches;
    }
}
