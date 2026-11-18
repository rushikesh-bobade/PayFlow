package com.payflow.service;

import com.payflow.dto.request.TransferRequest;
import com.payflow.entity.*;
import com.payflow.exception.InsufficientBalanceException;
import com.payflow.repository.LedgerEntryRepository;
import com.payflow.repository.TransactionRepository;
import com.payflow.repository.UserRepository;
import com.payflow.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TransactionServiceConcurrencyTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    private Wallet walletA;
    private Wallet walletB;

    @BeforeEach
    void setUp() {
        ledgerEntryRepository.deleteAll();
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        userRepository.deleteAll();

        User userA = userRepository.save(User.builder()
                .email("alice@payflow.internal")
                .password("hash123")
                .fullName("Alice Smith")
                .role(Role.ROLE_USER)
                .build());

        User userB = userRepository.save(User.builder()
                .email("bob@payflow.internal")
                .password("hash123")
                .fullName("Bob Jones")
                .role(Role.ROLE_USER)
                .build());

        walletA = walletRepository.save(Wallet.builder()
                .walletNumber("WLT-ALICE-100")
                .user(userA)
                .balance(new BigDecimal("100.0000"))
                .currency("USD")
                .status(WalletStatus.ACTIVE)
                .build());

        walletB = walletRepository.save(Wallet.builder()
                .walletNumber("WLT-BOB-000")
                .user(userB)
                .balance(new BigDecimal("0.0000"))
                .currency("USD")
                .status(WalletStatus.ACTIVE)
                .build());
    }

    @Test
    @DisplayName("Concurrency: 10 parallel threads attempting $20 transfers on a $100 balance prevents double-spending")
    void testConcurrentTransfersPreventDoubleSpend() throws InterruptedException {
        int threadCount = 10;
        BigDecimal transferAmount = new BigDecimal("20.00");
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // Synchronize all threads to fire simultaneously
                    TransferRequest request = TransferRequest.builder()
                            .targetWalletNumber(walletB.getWalletNumber())
                            .amount(transferAmount)
                            .description("Concurrent stress test transfer")
                            .build();

                    transactionService.transfer(walletA.getWalletNumber(), request);
                    successCount.incrementAndGet();
                } catch (InsufficientBalanceException ex) {
                    failureCount.incrementAndGet();
                } catch (Exception ex) {
                    // unexpected exception
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // Trigger simultaneous execution
        startLatch.countDown();
        boolean completed = endLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "All threads should complete within timeout");
        assertEquals(5, successCount.get(), "Exactly 5 transfers of $20 must succeed out of a $100 balance");
        assertEquals(5, failureCount.get(), "Exactly 5 transfers must fail due to InsufficientBalanceException");

        // Verify database state
        Wallet refreshedA = walletRepository.findById(walletA.getId()).orElseThrow();
        Wallet refreshedB = walletRepository.findById(walletB.getId()).orElseThrow();

        assertEquals(0, refreshedA.getBalance().compareTo(new BigDecimal("0.0000")), "Sender balance must be exactly $0");
        assertEquals(0, refreshedB.getBalance().compareTo(new BigDecimal("100.0000")), "Receiver balance must be exactly $100");
    }

    @Test
    @DisplayName("Deadlock Immunity: Bi-directional concurrent transfers between User A and User B execute without deadlock")
    void testBiDirectionalTransfersNoDeadlock() throws InterruptedException {
        int pairCount = 5;
        int totalThreads = pairCount * 2;
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(totalThreads);

        AtomicInteger totalSuccess = new AtomicInteger(0);

        // Pre-fund both wallets with $100 each
        walletB.setBalance(new BigDecimal("100.0000"));
        walletRepository.save(walletB);

        for (int i = 0; i < pairCount; i++) {
            // Thread A -> B
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TransferRequest req = TransferRequest.builder()
                            .targetWalletNumber(walletB.getWalletNumber())
                            .amount(new BigDecimal("10.00"))
                            .description("A to B")
                            .build();
                    transactionService.transfer(walletA.getWalletNumber(), req);
                    totalSuccess.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    endLatch.countDown();
                }
            });

            // Thread B -> A (Opposite direction concurrently)
            executor.submit(() -> {
                try {
                    startLatch.await();
                    TransferRequest req = TransferRequest.builder()
                            .targetWalletNumber(walletA.getWalletNumber())
                            .amount(new BigDecimal("10.00"))
                            .description("B to A")
                            .build();
                    transactionService.transfer(walletB.getWalletNumber(), req);
                    totalSuccess.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Bi-directional transfers must complete without hitting deadlock timeout");
        assertEquals(totalThreads, totalSuccess.get(), "All bi-directional transfers should succeed cleanly");
    }
}
