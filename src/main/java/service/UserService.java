package service;

import dto.UserDto;
import entity.UserEntity;
import mapper.UserMapper;
import repository.UserRepository;

import java.util.List;
import java.util.UUID;
import java.util.NoSuchElementException;

public class UserService {
  private final UserRepository repo;

  // Конструктор для подключения репозитория
  public UserService(UserRepository repo) {
    this.repo = repo;
  }

  // 1. Создать нового пользователя
  public UserDto createUser(UserDto dto) {
    UserEntity entity = UserMapper.toEntity(dto);
    repo.save(entity);
    return UserMapper.toDto(entity);
  }

  // 2. Получить пользователя по ID
  public UserDto getUser(UUID id) {
    return repo.findById(id)
        .map(UserMapper::toDto)
        .orElseThrow(() -> new NoSuchElementException("User not found"));
  }

  // 3. Получить всех пользователей
  public List<UserDto> getAllUsers() {
    return repo.findAll().stream()
        .map(UserMapper::toDto)
        .toList();
  }

  // 4. Удалить пользователя
  public void deleteUser(UUID id) {
    repo.deleteById(id);
  }

  // 5. Обновить данные пользователя (например, имя/почту)
  public UserDto updateUser(UUID id, UserDto dto) {
    UserEntity entity = repo.findById(id)
        .orElseThrow(() -> new NoSuchElementException("User not found"));

    entity.setName(dto.name());
    entity.setEmail(dto.email());
    entity.setPhoneNumber(dto.phoneNumber());

    repo.save(entity);
    return UserMapper.toDto(entity);
  }
}
