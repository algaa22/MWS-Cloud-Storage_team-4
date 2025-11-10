package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

import java.util.UUID;

public record GetFileInfoDto(UUID fileId, UUID userId) {
  public void validate() throws ValidationFailedException {
    ValidationResult result = Validators.all(
            Validators.notEmpty("File ID", fileId.toString()),
            Validators.notEmpty("User ID", userId.toString())
    );

    if (!result.isValid())
      throw new ValidationFailedException(result);
  }
}
