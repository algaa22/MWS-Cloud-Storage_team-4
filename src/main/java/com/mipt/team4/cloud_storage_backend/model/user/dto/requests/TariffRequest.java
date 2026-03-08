package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationError;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;

public record TariffRequest(
        String userToken,
        TariffPlan tariffPlan,
        String paymentToken,
        boolean autoRenew
) {
    public void validate(JwtService jwtService) throws ValidationFailedException {
        // 1. Базовая валидация через Validators
        ValidationResult result = Validators.all(
                Validators.notBlank("User token", userToken),
                Validators.notNull("Tariff plan", tariffPlan),
                Validators.notBlank("Payment token", paymentToken)
        );

        // Используем throwExceptionIfNotValid как в других DTO
        Validators.throwExceptionIfNotValid(result);

        // 2. Проверка токена через JwtService.isTokenValid()
        if (!jwtService.isTokenValid(userToken)) {
            throw new ValidationFailedException(
                    new ValidationError("userToken", "Invalid token")
            );
        }

        // 3. Проверка что это не пробный тариф
        if (tariffPlan.isTrial()) {
            throw new ValidationFailedException(
                    new ValidationError("tariffPlan", "Cannot purchase trial plan")
            );
        }
    }
}