package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

import java.util.List;
import java.util.Optional;

public record ChangeFileMetadataRequest(
        String userToken,
        String fileId,
        Optional<String> newName,
        Optional<String> newParentId,
        Optional<String> visibility,
        Optional<List<String>> tags) {

    public void validate(JwtService jwtService) throws ValidationFailedException {
        ValidationResult result =
                Validators.all(
                        Validators.validToken(jwtService, userToken),
                        Validators.notNull("File ID", fileId),
                        Validators.any(
                                "New file name",
                                "Invalid file name",
                                Validators.validate(newName.isEmpty(), null, null),
                                Validators.validFileName("New file name", newName.orElse(""))),
                        Validators.any(
                                "New file visibility",
                                "Invalid visibility",
                                Validators.validate(visibility.isEmpty(), null, null),
                                Validators.validateVisibility(visibility.orElse(null))),
                        Validators.any(
                                "New file metadata",
                                "One of the fields {New file Path, File visibility, File tags} must be specified",
                                Validators.validate(newName.isPresent() && !newName.get().isBlank(), null, null),
                                Validators.validate(newParentId.isPresent(), null, null),
                                Validators.validate(
                                        visibility.isPresent() && !visibility.get().isEmpty(), null, null),
                                Validators.validate(tags.isPresent(), null, null)));

        Validators.throwExceptionIfNotValid(result);
    }
}
