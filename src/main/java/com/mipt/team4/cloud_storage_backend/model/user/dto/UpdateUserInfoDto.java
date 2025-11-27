package com.mipt.team4.cloud_storage_backend.model.user.dto;

import java.util.Optional;

public record UpdateUserInfoDto (String accessToken, Optional<String> oldPassword, Optional<String> newPassword, Optional<String> newName) {}
