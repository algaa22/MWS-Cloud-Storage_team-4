package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.UserId;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@RequestMapping(method = "POST", path = ApiEndpoints.TARIFF_UPDATE_PAYMENT)
public record UpdatePaymentMethodRequest(
    @UserId UUID userId, @NotBlank @RequestHeader("X-Payment-Method-Id") String paymentMethodId) {}
