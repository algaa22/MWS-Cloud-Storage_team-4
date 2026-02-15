package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.List;

public record FileChunkedUploadDto(
    String sessionId, String userToken, String path, List<String> tags) {

  public void validate() throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.notBlank("Session ID", sessionId),
            Validators.validToken(userToken),
            Validators.mustBeFilePath("Path", path));

    Validators.throwExceptionIfNotValid(result);
  }
}
