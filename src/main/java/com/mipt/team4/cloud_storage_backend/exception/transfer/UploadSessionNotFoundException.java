package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Getter;

@Getter
public class UploadSessionNotFoundException extends BaseStorageException {
    RecoverableStorageException recoverableCause;

    public UploadSessionNotFoundException(RecoverableStorageException cause) {
        super("Upload session not found", cause, HttpResponseStatus.NOT_FOUND);
        this.recoverableCause = cause;
    }

    public UploadSessionNotFoundException() {
        super("Upload session not found", HttpResponseStatus.NOT_FOUND);
    }
}
