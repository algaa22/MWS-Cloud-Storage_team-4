package mapper;
import dto.FileDto;
import entity.FileEntity;
import java.util.UUID;

public class FileMapper {

  // DTO -> ENTITY
  public static FileEntity toEntity(FileDto dto) {
    return new FileEntity(
        UUID.randomUUID(),
        dto.name(),
        dto.size(),
        dto.path(),
        dto.url(),
        dto.tags()
    );
  }

  // ENTITY -> DTO
  public static FileDto toDto(FileEntity entity) {
    return new FileDto(
        entity.getName(),
        entity.getPath(),
        entity.getSize(),
        entity.getUrl(),
        entity.getTags()
    );
  }
}