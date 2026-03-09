package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class StorageObjectNotFoundException extends BaseStorageException {
    public StorageObjectNotFoundException(String message, Throwable cause) {
        super(message, cause, HttpResponseStatus.NOT_FOUND);
    }
}
