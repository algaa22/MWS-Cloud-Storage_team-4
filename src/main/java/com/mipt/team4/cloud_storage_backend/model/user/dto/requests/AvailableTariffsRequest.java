package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;

@RequestMapping(method = "GET", path = ApiEndpoints.TARIFF_PLANS)
public record AvailableTariffsRequest() {}
