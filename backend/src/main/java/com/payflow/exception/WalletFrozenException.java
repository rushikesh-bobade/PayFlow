package com.payflow.exception;

public class WalletFrozenException extends RuntimeException {
    public WalletFrozenException(String message) {
        super(message);
    }
}
