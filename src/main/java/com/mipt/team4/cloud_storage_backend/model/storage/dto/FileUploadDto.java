package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.List;

public record FileUploadDto(String path, String userToken, List<String> tags, byte[] data) {

  public void validate() throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.mustBeFilePath("File path", path),
            Validators.validToken(userToken),
            Validators.mustBePositive("File size", data.length));

    Validators.throwExceptionIfNotValid(result);
  }
}
