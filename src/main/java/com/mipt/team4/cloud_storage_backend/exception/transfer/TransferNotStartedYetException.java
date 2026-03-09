package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class TransferNotStartedYetException extends BaseStorageException {

    public TransferNotStartedYetException() {
        super("HttpContent received without active HttpRequest", HttpResponseStatus.BAD_REQUEST);
    }
}
