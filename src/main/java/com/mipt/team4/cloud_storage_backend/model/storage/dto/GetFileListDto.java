package com.mipt.team4.cloud_storage_backend.model.storage.dto;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import java.util.Optional;

public record GetFileListDto(
    String userToken,
    boolean includeDirectories,
    boolean recursive,
    Optional<String> searchDirectory) {

  public void validate() throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.validToken(userToken),
            Validators.notNull("Include directories", includeDirectories),
            Validators.notNull("Recursive", recursive),
            Validators.any( // TODO: 'specified' validation вынести в отдельную функцию
                "Search directory",
                "If search directory specified, it must be directory",
                Validators.validate(searchDirectory.isEmpty(), null, null),
                Validators.mustBeDirectoryPath("Search directory", searchDirectory.orElse(null))));

    Validators.throwExceptionIfNotValid(result);
  }
}
