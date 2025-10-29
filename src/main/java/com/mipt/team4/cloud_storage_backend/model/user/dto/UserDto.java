package com.mipt.team4.cloud_storage_backend.model.user.dto;

public record UserDto(
    String name, String email, String surname, String phoneNumber, long freeSpace) {}
