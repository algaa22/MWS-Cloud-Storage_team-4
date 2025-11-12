package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record GetFileInfoDto(String fileId, String userId) {
  public void validate() throws ValidationFailedException {
    ValidationResult result = Validators.all(
            Validators.isUuid("File ID", fileId),
            Validators.isUuid("User ID", userId)
    );

    Validators.throwExceptionIfNotValid(result);
  }
}
