package com.mipt.team4.cloud_storage_backend.model.user.dto;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record RegisterRequestDto (String email, String phoneNumber, String passwordHash, String userName) {
  public void validate() throws ValidationFailedException {
    ValidationResult result =
            Validators.all(
                    Validators.notBlank("Email", email), // TODO: pattern
                    Validators.notBlank("Phone number", phoneNumber), // TODO: pattern
                    Validators.notBlank("Password hash", passwordHash),
                    Validators.notBlank("User name", userName));

    Validators.throwExceptionIfNotValid(result);
  }
}
