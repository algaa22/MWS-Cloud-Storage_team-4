package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.service.user.security.AccessTokenService;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.List;
import java.util.Optional;

public record StartChunkedUploadRequest(
    String sessionId,
    String userToken,
    Optional<String> parentId,
    String name,
    List<String> tags,
    int size) {

  public void validate(AccessTokenService accessTokenService) throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.mustBePositive("File size", size),
            Validators.notBlank("Session ID", sessionId),
            Validators.validToken(accessTokenService, userToken),
            Validators.validFileName("File Name", name));

    Validators.throwExceptionIfNotValid(result);
  }
}
