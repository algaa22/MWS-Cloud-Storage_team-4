package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.List;

public record FileChunkedUploadDto(
    String sessionId,
    String userToken,
    long totalFileSize,
    int totalChunks,
    String path,
    List<String> tags) {

  public void validate() throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.notBlank("Session ID", sessionId),
            Validators.validToken(userToken),
            Validators.notBlank("Path", path),
            Validators.mustBePositive("Total file size", totalFileSize),
            Validators.numberMax(
                "Total file size", totalFileSize, StorageConfig.INSTANCE.getMaxFileSize()),
            Validators.mustBePositive("Total chunks", totalChunks));

    Validators.throwExceptionIfNotValid(result);
  }
}
