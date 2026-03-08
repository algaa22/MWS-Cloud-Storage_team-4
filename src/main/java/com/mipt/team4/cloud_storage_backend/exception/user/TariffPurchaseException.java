package com.mipt.team4.cloud_storage_backend.exception.user;

public class TariffPurchaseException extends RuntimeException {

    public TariffPurchaseException(String message) {
        super(message);
    }

    public TariffPurchaseException(String message, Throwable cause) {
        super(message, cause);
    }
}