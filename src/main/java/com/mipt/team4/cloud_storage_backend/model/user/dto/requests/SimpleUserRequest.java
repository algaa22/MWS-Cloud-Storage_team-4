package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record SimpleUserRequest(String token) {

  public void validate(JwtService jwtService) throws ValidationFailedException {
    ValidationResult result = Validators.all(Validators.validToken(jwtService, token));

    Validators.throwExceptionIfNotValid(result);
  }
}
