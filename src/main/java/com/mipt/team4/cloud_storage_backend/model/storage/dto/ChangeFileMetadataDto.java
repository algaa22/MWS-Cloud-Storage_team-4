package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.List;
import java.util.Optional;

public record ChangeFileMetadataDto(
    String userToken,
    String oldPath,
    Optional<String> newPath,
    Optional<Boolean> visibility,
    Optional<List<String>> tags) {
  public void validate() throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.validToken(userToken),
            Validators.notBlank("Old file path", oldPath),
            Validators.any(
                "File metadata",
                "One of the fields {newPath, visibility, tags} must not be null",
                Validators.validate(newPath.isPresent() && !newPath.get().isEmpty(), "New file path"),
                Validators.validate(visibility.isPresent(), "File visibility"),
                Validators.validate(tags.isPresent() && !tags.get().isEmpty(), "File tags")));

    Validators.throwExceptionIfNotValid(result);
  }
}
