package com.payflow.repository;

import com.payflow.entity.LedgerEntry;
import com.payflow.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    Optional<LedgerEntry> findByEntryId(String entryId);

    Page<LedgerEntry> findAllByWalletOrderByCreatedAtDesc(Wallet wallet, Pageable pageable);

    Page<LedgerEntry> findAllByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

    /**
     * Financial Ledger Invariant Check:
     * Sum(Credits) - Sum(Debits) must equal the current wallet balance.
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN l.type = com.payflow.entity.LedgerEntryType.CREDIT THEN l.amount ELSE -l.amount END), 0.0) " +
           "FROM LedgerEntry l WHERE l.wallet.id = :walletId")
    BigDecimal calculateLedgerNetBalance(@Param("walletId") Long walletId);
}
