package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.Optional;

public record UpdateUserInfoRequest(
    String userToken,
    Optional<String> oldPassword,
    Optional<String> newPassword,
    Optional<String> newName) {

  public void validate(JwtService jwtService) throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.validToken(jwtService, userToken),
            Validators.any(
                "New user info",
                "One of the fields {NewUsername, newPassword} must be specified",
                Validators.validate(newName.isPresent(), null, null),
                Validators.validate(newPassword.isPresent(), null, null)),
            Validators.any(
                "New password",
                "If new password specified, old password also must be specified",
                Validators.validate(newPassword.isEmpty(), null, null),
                Validators.validate(
                    oldPassword.isPresent() && !oldPassword.get().isEmpty(), null, null)));

    Validators.throwExceptionIfNotValid(result);
  }
}
