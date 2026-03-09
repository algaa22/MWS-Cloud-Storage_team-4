package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationError;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record UpdateAutoRenewRequest(String userToken, String paymentMethodId) {
    public void validate(JwtService jwtService) throws ValidationFailedException {
        ValidationResult result = Validators.all(Validators.notBlank("User token", userToken));

        Validators.throwExceptionIfNotValid(result);

        if (!jwtService.isTokenValid(userToken)) {
            throw new ValidationFailedException(new ValidationError("userToken", "Invalid token"));
        }
    }
}
