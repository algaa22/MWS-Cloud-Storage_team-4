package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.constants.ApiEndpoints;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestMapping;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.UserId;
import java.util.UUID;

@RequestMapping(method = "GET", path = ApiEndpoints.USERS_INFO)
public record UserInfoRequest(@UserId UUID userId) {}
