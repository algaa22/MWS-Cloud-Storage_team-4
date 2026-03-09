package com.mipt.team4.cloud_storage_backend.exception.retry;

public class UploadRetriableException extends RetriableException {
    public UploadRetriableException(String message, Throwable cause) {
        super(message, cause);
    }

    public UploadRetriableException(Throwable cause) {
        super("UPLOAD", cause);
    }
}
