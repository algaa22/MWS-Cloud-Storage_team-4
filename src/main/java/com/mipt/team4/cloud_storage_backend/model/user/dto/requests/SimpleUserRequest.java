package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.service.user.security.AccessTokenService;

public record SimpleUserRequest(String token) {

  public void validate(AccessTokenService accessTokenService) throws ValidationFailedException {
    ValidationResult result = Validators.all(Validators.validToken(accessTokenService, token));

    Validators.throwExceptionIfNotValid(result);
  }
}
