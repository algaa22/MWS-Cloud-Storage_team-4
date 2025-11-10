package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record GetFileInfoRequest(String fileId, String userId) {
  public void validate() throws ValidationFailedException {
    ValidationResult result = Validators.all(
            Validators.notEmpty("File ID", fileId),
            Validators.notEmpty("User ID", userId)
    );

    if (!result.isValid())
      throw new ValidationFailedException(result);
  }
}
