package com.mipt.team4.cloud_storage_backend.exception.netty;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class NotHttpRequestException extends FatalStorageException {
    public NotHttpRequestException() {
        super("Unable to handle not http request");
    }
}
