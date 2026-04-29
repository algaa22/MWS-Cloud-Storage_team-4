package com.mipt.team4.cloud_storage_backend.model.share.dto;

import com.mipt.team4.cloud_storage_backend.config.constants.netty.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;

@RequestMapping(method = "GET", path = ApiEndpoints.SHARES_DOWNLOAD)
public record PublicShareRequest(
    @QueryParam("shareToken") String token,
    @RequestHeader(required = false) String sharePassword) {}
