package com.mipt.team4.cloud_storage_backend.model.user.dto.requests;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestHeader;

public record TokenActionRequest(@RequestHeader("X-Auth-Token") String token) {}
