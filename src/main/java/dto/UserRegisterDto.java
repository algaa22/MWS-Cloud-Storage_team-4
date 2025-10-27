package dto;

public record UserRegisterDto(
  String name,
  String email,
  String surname,
  String phoneNumber,
  String password
) {}
