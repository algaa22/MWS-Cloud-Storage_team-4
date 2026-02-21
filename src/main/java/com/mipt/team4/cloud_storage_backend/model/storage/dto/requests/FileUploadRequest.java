package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.List;
import java.util.UUID;

public record FileUploadRequest(
    UUID parentId, String name, String userToken, List<String> tags, byte[] data) {

  public void validate(JwtService jwtService) throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.validFileName("File name", name),
            Validators.validToken(jwtService, userToken),
            Validators.mustBePositive("File size", data.length));

    Validators.throwExceptionIfNotValid(result);
  }
}
