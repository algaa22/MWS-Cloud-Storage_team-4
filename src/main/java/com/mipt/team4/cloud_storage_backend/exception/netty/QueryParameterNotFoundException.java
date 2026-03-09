package com.mipt.team4.cloud_storage_backend.exception.netty;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;

public class QueryParameterNotFoundException extends BaseStorageException {

    public QueryParameterNotFoundException(String paramName) {
        super("Missing required parameter: " + paramName, HttpResponseStatus.BAD_REQUEST);
    }
}
