package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

import java.util.UUID;

public record FileChunkDto(UUID sessionId, String path, int chunkIndex, byte[] chunkData) {
  public void validate() throws ValidationFailedException {
    // TODO: доделать валидацию
    ValidationResult result = Validators.all(
            Validators.notEmpty("Session ID", sessionId.toString()),
            Validators.notNull("Chunk data", chunkData),
            Validators.mustBePositive("Chunk data", chunkData.length),
            Validators.cannotBeNegative("Chunk index", chunkIndex)
    );

    if (!result.isValid())
      throw new ValidationFailedException(result);
  }
}
