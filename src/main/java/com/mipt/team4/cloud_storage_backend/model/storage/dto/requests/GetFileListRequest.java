package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.service.user.security.AccessTokenService;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.Optional;

public record GetFileListRequest(
    String userToken,
    boolean includeDirectories,
    boolean recursive,
    Optional<String> parentId,
    Optional<String> tags) {
  public void validate(AccessTokenService accessTokenService) throws ValidationFailedException {
    ValidationResult result = Validators.all(Validators.validToken(accessTokenService, userToken));
    Validators.throwExceptionIfNotValid(result);
  }
}
