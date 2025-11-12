package com.mipt.team4.cloud_storage_backend.model.user.dto;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record LogoutRequestDto(String token) {
  public void validate() throws ValidationFailedException {
    ValidationResult result = Validators.all(
            Validators.notBlank("Token", token)
    );

    Validators.throwExceptionIfNotValid(result);
  }
}
