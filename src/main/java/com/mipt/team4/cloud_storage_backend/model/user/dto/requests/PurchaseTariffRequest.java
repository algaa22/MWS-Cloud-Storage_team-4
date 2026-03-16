package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.model.user.enums.TariffPlan;
import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.constants.ValidationPatterns;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

@RequestMapping(method = "POST", path = ApiEndpoints.TARIFF_PURCHASE)
public record PurchaseTariffRequest(
    @UserId UUID userId,
    @NotNull @QueryParam("plan") TariffPlan plan,
    @NotBlank
        @Pattern(
            regexp = ValidationPatterns.PAYMENT_TOKEN_REGEXP,
            message = ValidationPatterns.PAYMENT_TOKEN_ERROR)
        @RequestHeader("X-Payment-Token")
        String paymentToken,
    @QueryParam(value = "autoRenew", defaultValue = "true") boolean autoRenew,
    @RequestHeader(value = "X-Payment-Method", defaultValue = "card") String paymentMethod) {}
