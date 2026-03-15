package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.UserId;
import java.util.UUID;

public record UserActionRequest(@UserId UUID userId) {}
