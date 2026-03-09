package com.mipt.team4.cloud_storage_backend.exception.storage;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class MissingFilePartException extends BaseStorageException {

    public MissingFilePartException(int index) {
        super("Missing file part #" + index, HttpResponseStatus.BAD_REQUEST);
    }
}
