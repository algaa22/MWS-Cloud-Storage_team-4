package mapper;
import dto.UserDto;
import entity.UserEntity;
import java.util.UUID;

public class UserMapper {

  // DTO -> ENTITY
  public static UserEntity toEntity(UserDto dto) {
    return new UserEntity(
        UUID.randomUUID(),
        dto.name(),
        dto.email(),
        dto.phoneNumber(),
        dto.surname()
    );
  }

  // ENTITY -> DTO
  public static UserDto toDto(UserEntity entity) {
    return new UserDto(
        entity.getName(),
        entity.getEmail(),
        entity.getSurname(),
        entity.getPhoneNumber()
    );
  }
}