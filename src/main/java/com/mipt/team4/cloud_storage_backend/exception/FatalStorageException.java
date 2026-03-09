package com.mipt.team4.cloud_storage_backend.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

public class FatalStorageException extends BaseStorageException {
    public FatalStorageException(String message, Throwable cause) {
        super(message, cause, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    public FatalStorageException(String message) {
        super(message, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    public FatalStorageException(Throwable cause) {
        super("Unknown exception caught", cause, HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }
}
