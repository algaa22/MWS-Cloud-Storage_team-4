package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record FileChunkDto(String sessionId, String path, int chunkIndex, byte[] chunkData) {
  public void validate() throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.isUuid("Session ID", sessionId),
            Validators.notNull("Chunk data", chunkData),
            Validators.mustBePositive("Chunk data", chunkData.length),
            Validators.cannotBeNegative("Chunk index", chunkIndex));

    Validators.throwExceptionIfNotValid(result);
  }
}
