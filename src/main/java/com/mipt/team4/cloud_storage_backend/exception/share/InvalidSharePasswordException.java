package com.mipt.team4.cloud_storage_backend.exception.share;

public class InvalidSharePasswordException extends RuntimeException {
    public InvalidSharePasswordException() {
        super("Invalid password for share link");
    }
}