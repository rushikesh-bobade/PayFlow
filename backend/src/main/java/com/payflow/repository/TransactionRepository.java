package com.payflow.repository;

import com.payflow.entity.Transaction;
import com.payflow.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceId(String referenceId);

    @Query("SELECT t FROM Transaction t WHERE t.sourceWallet = :wallet OR t.targetWallet = :wallet ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByWallet(@Param("wallet") Wallet wallet, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.sourceWallet.id = :walletId OR t.targetWallet.id = :walletId ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByWalletId(@Param("walletId") Long walletId, Pageable pageable);
}
