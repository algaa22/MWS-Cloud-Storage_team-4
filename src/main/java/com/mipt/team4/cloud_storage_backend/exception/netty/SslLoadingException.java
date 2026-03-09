package com.mipt.team4.cloud_storage_backend.exception.netty;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class SslLoadingException extends FatalStorageException {
    public SslLoadingException(Throwable cause) {
        super("Failed to load SSL from PKCS12 file", cause);
    }
}
