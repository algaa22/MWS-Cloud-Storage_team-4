package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

import java.util.List;

public record SearchFilesByTagsRequest(String userToken, List<String> tags) {

    public void validate(JwtService jwtService) throws ValidationFailedException {
        ValidationResult result =
                Validators.all(
                        Validators.validToken(jwtService, userToken),
                        Validators.notNull("Tags", tags),
                        Validators.validate(
                                tags != null && !tags.isEmpty(), "Tags list must not be empty", null));

        Validators.throwExceptionIfNotValid(result);
    }
}
