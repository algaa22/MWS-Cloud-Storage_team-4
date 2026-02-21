package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.UUID;

public record SimpleFileOperationRequest(UUID parentId, String name, String userToken) {

  public void validate(JwtService jwtService) throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.notNull("Parent ID", parentId),
            Validators.validFileName("File name", name),
            Validators.validToken(jwtService, userToken));

    Validators.throwExceptionIfNotValid(result);
  }
}
