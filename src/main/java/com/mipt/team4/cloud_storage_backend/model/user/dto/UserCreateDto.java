package com.mipt.team4.cloud_storage_backend.model.user.dto;

public record UserCreateDto(
    String name, String email, String surname, String phoneNumber, String password) {}
