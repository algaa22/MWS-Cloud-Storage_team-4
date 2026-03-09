package com.mipt.team4.cloud_storage_backend.exception.user;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class InvalidEmailOrPassword extends BaseStorageException {

    public InvalidEmailOrPassword() {
        super("No such user", HttpResponseStatus.NOT_FOUND);
    }
}
