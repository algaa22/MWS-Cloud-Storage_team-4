package com.mipt.team4.cloud_storage_backend.model.share.dto;

import com.mipt.team4.cloud_storage_backend.config.constants.netty.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import java.util.UUID;

@RequestMapping(method = "DELETE", path = ApiEndpoints.SHARES_PREFIX + "/permanent")
public record DeleteShareRequest(@UserId UUID userId, @QueryParam UUID shareId) {}
