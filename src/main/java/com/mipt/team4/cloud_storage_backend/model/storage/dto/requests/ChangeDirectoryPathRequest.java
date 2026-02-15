package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record ChangeDirectoryPathRequest(
    String userToken, String oldDirectoryPath, String newDirectoryPath) {

  public void validate(JwtService jwtService) throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.validToken(jwtService, userToken),
            Validators.mustBeDirectory("Old directory path", oldDirectoryPath),
            Validators.mustBeDirectory("New directory path", newDirectoryPath),
            Validators.notEqualIgnoreCase(
                "Old and new directory paths", oldDirectoryPath, newDirectoryPath));

    Validators.throwExceptionIfNotValid(result);
  }
}
