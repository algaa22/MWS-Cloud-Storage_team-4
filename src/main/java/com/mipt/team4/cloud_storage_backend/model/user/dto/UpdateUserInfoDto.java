package com.mipt.team4.cloud_storage_backend.model.user.dto;

public record UpdateUserInfoDto (String accessToken, String oldPassword, String newPassword, String newName) {}
