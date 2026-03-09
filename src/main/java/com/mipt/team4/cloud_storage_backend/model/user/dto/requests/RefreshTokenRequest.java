package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record RefreshTokenRequest(String refreshToken) {

  public void validate() throws ValidationFailedException {
    ValidationResult result = Validators.all(Validators.notBlank("Refresh token", refreshToken));

    Validators.throwExceptionIfNotValid(result);
  }
}
