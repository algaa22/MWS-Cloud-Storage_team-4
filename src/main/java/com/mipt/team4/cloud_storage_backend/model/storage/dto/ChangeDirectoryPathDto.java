package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record ChangeDirectoryPathDto(
    String userToken, String oldDirectoryPath, String newDirectoryPath) {

  public void validate() throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.validToken(userToken),
            Validators.mustBeDirectory("Old directory path", oldDirectoryPath),
            Validators.mustBeDirectory("New directory path", newDirectoryPath),
            Validators.notEqualIgnoreCase(
                "Old and new directory paths", oldDirectoryPath, newDirectoryPath));

    Validators.throwExceptionIfNotValid(result);
  }
}
