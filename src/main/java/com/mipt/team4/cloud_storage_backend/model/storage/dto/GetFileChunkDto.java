package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record GetFileChunkDto(String fileId, int chunkIndex, int chunkSize) {
  public void validate() throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.isUuid("File ID", fileId),
            Validators.cannotBeNegative("Chunk index", chunkIndex),
            Validators.numberMax(
                "Chunk size", chunkSize, StorageConfig.INSTANCE.getMaxFileChunkSize()));

    Validators.throwExceptionIfNotValid(result);
  }
}
