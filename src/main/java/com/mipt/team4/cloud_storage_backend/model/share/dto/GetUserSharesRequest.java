package com.mipt.team4.cloud_storage_backend.model.share.dto;

import com.mipt.team4.cloud_storage_backend.config.constants.netty.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import java.util.UUID;

@RequestMapping(method = "GET", path = ApiEndpoints.SHARES_USER)
public record GetUserSharesRequest(@UserId UUID userId) {}
