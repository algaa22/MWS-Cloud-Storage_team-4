package mapper;
import dto.UserGetDto;
import dto.UserRegisterDto;
import entity.UserEntity;
import java.util.UUID;

public class UserMapper {

  // DTO -> ENTITY
  public static UserEntity toEntity(UserRegisterDto dto) {
    return new UserEntity(
        UUID.randomUUID(),
        dto.name(),
        dto.email(),
        dto.password(),
        dto.phoneNumber(),
        dto.surname(),
        null
    );
  }

  // ENTITY -> DTO
  public static UserGetDto toDto(UserEntity entity) {
    return new UserGetDto(
        entity.getName(),
        entity.getEmail(),
        entity.getSurname(),
        entity.getPhoneNumber(),
        entity.getFreeSpace()
    );
  }
}