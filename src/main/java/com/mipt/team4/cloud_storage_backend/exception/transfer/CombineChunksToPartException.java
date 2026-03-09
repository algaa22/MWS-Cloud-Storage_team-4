package com.mipt.team4.cloud_storage_backend.exception.transfer;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;

public class CombineChunksToPartException extends FatalStorageException {

    public CombineChunksToPartException(Throwable cause) {
        super("Failed to combine chunks to part", cause);
    }
}
