package com.mipt.team4.cloud_storage_backend.model.storage.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.service.user.security.AccessTokenService;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record MoveDirectoryRequest(String userToken, String directoryId, String newParentId) {
  public void validate(AccessTokenService accessTokenService) throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.validToken(accessTokenService, userToken),
            Validators.isUuid("Directory ID", directoryId),
            Validators.isUuid("New Parent ID", newParentId));
    Validators.throwExceptionIfNotValid(result);
  }
}
