package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record ResumeChunkedUploadRequest(String sessionId) {
  public void validate() throws ValidationFailedException {
    ValidationResult result = Validators.all(Validators.notBlank("Session ID", sessionId));

    Validators.throwExceptionIfNotValid(result);
  }
}
