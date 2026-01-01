package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record UploadChunkDto(String sessionId, String path, int chunkIndex, byte[] chunkData) {

  public void validate() throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.isUuid("Session ID", sessionId),
            Validators.mustBeFilePath("Path", path),
            Validators.notNull("Chunk data", chunkData),
            Validators.mustBePositive("Chunk data size", chunkData.length),
            Validators.numberMax("Chunk size", chunkData.length,
                StorageConfig.INSTANCE.getMaxFileChunkSize()),
            Validators.cannotBeNegative("Chunk index", chunkIndex));

    Validators.throwExceptionIfNotValid(result);
  }
}
