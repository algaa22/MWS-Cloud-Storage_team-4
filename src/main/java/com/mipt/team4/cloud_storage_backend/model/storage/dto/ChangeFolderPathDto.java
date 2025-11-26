package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record ChangeFolderPathDto(String userToken, String oldFolderPath, String newOlderPath) {
  public void validate() throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.validToken(userToken),
            Validators.mustBeFilePath("Old folder path", oldFolderPath),
            Validators.mustBeFilePath("New folder path", newOlderPath));

    Validators.throwExceptionIfNotValid(result);
  }
}
