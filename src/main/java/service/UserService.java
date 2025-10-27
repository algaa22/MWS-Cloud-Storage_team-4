package service;

import dto.UserGetDto;
import java.util.List;
import java.util.UUID;

public interface UserService {
  // Принимать UserCreateDto на создание, возвращать UserGetDto
  UserGetDto createUser(UserGetDto dto);

  // Получить данные пользователя по id
  UserGetDto getUser(UUID id);

  // Получить список всех пользователей
  List<UserGetDto> getAllUsers();

  // Удалить пользователя по id
  void deleteUser(UUID id);

  // Обновить данные пользователя
  //UserGetDto updateUser(UUID id, UserGetDto dto);
}

