package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.List;
import java.util.UUID;

public record FileChunkedUploadDto(
    UUID sessionId,
    UUID ownerId,
    long totalFileSize,
    int totalChunks,
    String path,
    List<String> tags) {

  public void validate() throws ValidationFailedException {
    // TODO: доделать валидацию
    ValidationResult result =
        Validators.all(
            Validators.notEmpty("Session ID", sessionId.toString()),
            Validators.notEmpty("Owner ID", ownerId.toString()),
            Validators.notEmpty("Path", path),
            Validators.mustBePositive("Total file size", totalFileSize),
            Validators.numberMax(
                "Total file size", totalFileSize, StorageConfig.getInstance().getMaxFileSize()),
            Validators.mustBePositive("Total chunks", totalChunks));

    if (!result.isValid()) throw new ValidationFailedException(result);
  }
}
