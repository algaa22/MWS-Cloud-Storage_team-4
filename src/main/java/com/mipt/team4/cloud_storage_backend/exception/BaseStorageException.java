package com.mipt.team4.cloud_storage_backend.exception;

import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;

@Getter
public abstract class BaseStorageException extends RuntimeException {
    private final HttpResponseStatus status;

    public BaseStorageException(String message, Throwable cause, HttpResponseStatus status) {
        super(message, cause);
        this.status = status;
    }

    public BaseStorageException(String message, HttpResponseStatus status) {
        super(message);
        this.status = status;
    }
}
