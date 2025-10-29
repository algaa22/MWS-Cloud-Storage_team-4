package com.mipt.team4.cloudstorage.model.user.dto;

public record UserCreateDto(
    String name, String email, String surname, String phoneNumber, String password) {}
