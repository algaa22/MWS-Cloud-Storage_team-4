package com.mipt.team4.cloud_storage_backend.model.share.dto;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestBody;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;

@RequestMapping(method = "POST", path = ApiEndpoints.SHARES_VALIDATE_PASSWORD)
public record ValidatePasswordRequest(
        @QueryParam String shareToken,
        @RequestBody String password
) {}