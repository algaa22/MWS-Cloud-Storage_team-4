package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record SimpleFileOperationDto(String filePath, String userId) {
  public void validate() throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.notBlank("File path", filePath), Validators.isUuid("User ID", userId));

    Validators.throwExceptionIfNotValid(result);
  }
}
