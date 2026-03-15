package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.service.user.security.AccessTokenService;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationError;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import com.mipt.team4.cloud_storage_backend.utils.validation.Validators;
import lombok.Data;

@Data
public record PurchaseTariffRequest(
    String userToken,
    TariffPlan tariffPlan,
    String paymentToken,
    boolean autoRenew,
    String paymentMethod) {
  public void validate(AccessTokenService accessTokenService) throws ValidationFailedException {
    ValidationResult result =
        Validators.all(
            Validators.notBlank("User token", userToken),
            Validators.notNull("Tariff plan", tariffPlan),
            Validators.notBlank("Payment token", paymentToken));

    Validators.throwExceptionIfNotValid(result);
    if (!accessTokenService.isTokenValid(userToken)) {
      throw new ValidationFailedException(new ValidationError("userToken", "Invalid token"));
    }

    if (tariffPlan.isTrial()) {
      throw new ValidationFailedException(
          new ValidationError("tariffPlan", "Cannot purchase trial plan"));
    }
  }
}
