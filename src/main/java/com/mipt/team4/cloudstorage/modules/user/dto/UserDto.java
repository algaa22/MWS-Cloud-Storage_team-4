package com.mipt.team4.cloudstorage.modules.user.dto;

public record UserDto(
    String name, String email, String surname, String phoneNumber, long freeSpace) {}
