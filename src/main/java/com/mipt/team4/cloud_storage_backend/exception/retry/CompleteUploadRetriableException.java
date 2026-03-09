package com.mipt.team4.cloud_storage_backend.exception.retry;

public class CompleteUploadRetriableException extends UploadRetriableException {
    public CompleteUploadRetriableException(Throwable cause) {
        super("COMPLETE UPLOAD", cause);
    }
}
