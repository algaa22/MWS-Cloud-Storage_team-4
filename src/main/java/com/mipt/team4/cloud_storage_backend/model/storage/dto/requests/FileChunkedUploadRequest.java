package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.List;
import java.util.Optional;

public record FileChunkedUploadRequest(
    String sessionId, String userToken, Optional<String> parentId, String name, List<String> tags) {

  public void validate(JwtService jwtService) throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.notBlank("Session ID", sessionId),
            Validators.validToken(jwtService, userToken),
            Validators.validFileName("File Name", name));

    Validators.throwExceptionIfNotValid(result);
  }
}
