package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.UUID;

public record CreateDirectoryRequest(
    String userToken, UUID parentId, String name, UUID directoryId) {

  public void validate(JwtService jwtService) throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.validFileName("Directory name", name),
            Validators.validToken(jwtService, userToken),
            Validators.notNull("Directory id", directoryId));

    Validators.throwExceptionIfNotValid(result);
  }
}
