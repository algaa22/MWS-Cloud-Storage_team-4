package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

import java.util.List;

public record FileChunkedUploadSession(
    String sessionId,
    String ownerId,
    long totalFileSize,
    int totalChunks,
    String path,
    List<String> tags,
    List<FileChunk> chunks) {
  public void validate() throws ValidationFailedException {
    // TODO: доделать валидацию
    ValidationResult result = Validators.all(
            Validators.notEmpty("Session ID", sessionId),
            Validators.notEmpty("Owner ID", ownerId),
            Validators.notEmpty("Path", path),
            Validators.mustBePositive("Total file size", totalFileSize),
            Validators.numberMax("Total file size", totalFileSize, StorageConfig.getInstance().getMaxFileSize()),
            Validators.mustBePositive("Total chunks", totalChunks)
    );

    if (!result.isValid())
      throw new ValidationFailedException(result);
  }
}
