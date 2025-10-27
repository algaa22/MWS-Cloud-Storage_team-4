package dto;

public record UserGetDto(
    String name,
    String email,
    String surname,
    String phoneNumber,
    long freeSpace
    ) {}