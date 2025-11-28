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
    Optional<String> visibility,
    Optional<List<String>> tags) {
  public void validate() throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.validToken(userToken),
            Validators.mustBeFilePath("Old file path", oldPath),
            Validators.any(
                "New file path",
                "If new file path specified, it must not be directory",
                Validators.validate(newPath.isEmpty(), null, null),
                Validators.mustBeFilePath(null, newPath.orElse(null))),
                Validators.any(
                        "New file visibility",
                        "If new file visibility specified, it must be has valid value",
                        Validators.validate(visibility.isEmpty(), null, null),
                        Validators.validateVisibility(visibility.orElse(null))),
            Validators.any(
                "New file metadata",
                "One of the fields {New file Path, File visibility, File tags} must be specified",
                Validators.validate(newPath.isPresent() && !newPath.get().isEmpty(), null, null),
                Validators.validate(
                    visibility.isPresent() && !visibility.get().isEmpty(), null, null),
                Validators.validate(tags.isPresent() && !tags.get().isEmpty(), null, null)));

    Validators.throwExceptionIfNotValid(result);
  }
}
