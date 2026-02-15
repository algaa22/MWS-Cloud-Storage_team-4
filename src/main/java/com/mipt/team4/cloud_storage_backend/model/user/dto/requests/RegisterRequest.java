package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record RegisterRequest(String email, String password, String userName) {

  public void validate() throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.isEmail(email),
            Validators.notBlank("Password hash", password),
            Validators.notBlank("User name", userName));

    Validators.throwExceptionIfNotValid(result);
  }
}
