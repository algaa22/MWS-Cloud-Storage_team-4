package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record GetFileChunkDto(String userToken, String filePath, int chunkIndex) {
  public void validate() throws ValidationFailedException {
    ValidationResult result =
        Validators.all(Validators.validToken(userToken), Validators.isUuid("File path", filePath));

    Validators.throwExceptionIfNotValid(result);
  }
}
