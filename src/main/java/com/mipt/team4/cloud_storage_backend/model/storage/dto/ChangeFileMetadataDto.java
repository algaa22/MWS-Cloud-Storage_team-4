package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.List;
import java.util.Optional;

public record ChangeFileMetadataDto(
    String userToken,
    Optional<String> path,
    Optional<Boolean> visibility,
    Optional<List<String>> tags) {
  public void validate() throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.validToken(userToken),
            Validators.any(
                "File metadata",
                "One of the fields {path, visibility, tags} must not be null",
                Validators.validate(path.isPresent() && !path.get().isEmpty(), "File path"),
                Validators.validate(visibility.isPresent(), "File visibility"),
                Validators.validate(tags.isPresent() && !tags.get().isEmpty(), "File tags")));

    Validators.throwExceptionIfNotValid(result);
  }
}
