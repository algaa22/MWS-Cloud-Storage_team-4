package com.mipt.team4.cloud_storage_backend.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

public class RecoverableStorageException extends BaseStorageException {
    public RecoverableStorageException(Throwable cause) {
        super("Temporary issue", cause, HttpResponseStatus.SERVICE_UNAVAILABLE);
    }
}
