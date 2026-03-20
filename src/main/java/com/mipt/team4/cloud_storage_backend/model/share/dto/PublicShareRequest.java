package com.mipt.team4.cloud_storage_backend.model.share.dto;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestBodyParam;

@RequestMapping(method = "GET", path = ApiEndpoints.SHARES_DOWNLOAD)
public record PublicShareRequest(
        @RequestBodyParam("token") String token
) {}