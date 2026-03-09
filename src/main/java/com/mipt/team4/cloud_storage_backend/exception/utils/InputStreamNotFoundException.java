package com.mipt.team4.cloud_storage_backend.exception.utils;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class InputStreamNotFoundException extends FatalStorageException {

    public InputStreamNotFoundException(String filePath, Throwable cause) {
        super("File " + filePath + " not found in filesystem or classpath", cause);
    }
}
