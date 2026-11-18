package com.payflow.service;

import com.payflow.dto.response.WalletResponse;
import com.payflow.entity.*;
import com.payflow.exception.WalletFrozenException;
import com.payflow.repository.LedgerEntryRepository;
import com.payflow.repository.TransactionRepository;
import com.payflow.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private WalletService walletService;

    private User user;
    private Wallet wallet;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("test@payflow.internal")
                .fullName("Test User")
                .role(Role.ROLE_USER)
                .build();

        wallet = Wallet.builder()
                .id(10L)
                .walletNumber("WLT1234567890")
                .user(user)
                .balance(new BigDecimal("250.0000"))
                .currency("USD")
                .status(WalletStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Deposit: Valid deposit increases wallet balance and creates ledger record")
    void testDepositSuccess() {
        when(walletRepository.findByWalletNumberForUpdate("WLT1234567890")).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerEntryRepository.calculateLedgerNetBalance(10L)).thenReturn(new BigDecimal("350.0000"));

        WalletResponse response = walletService.deposit("WLT1234567890", new BigDecimal("100.00"), "Test Deposit");

        assertNotNull(response);
        assertEquals(0, response.getBalance().compareTo(new BigDecimal("350.00")));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(ledgerService, times(1)).recordCredit(any(), eq(wallet), eq(new BigDecimal("100.00")), anyString());
    }

    @Test
    @DisplayName("Deposit: Frozen wallet rejects deposit with WalletFrozenException")
    void testDepositFailsOnFrozenWallet() {
        wallet.setStatus(WalletStatus.FROZEN);
        when(walletRepository.findByWalletNumberForUpdate("WLT1234567890")).thenReturn(Optional.of(wallet));

        assertThrows(WalletFrozenException.class, () ->
                walletService.deposit("WLT1234567890", new BigDecimal("50.00"), "Deposit attempt"));
    }
}
