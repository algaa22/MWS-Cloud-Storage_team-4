package mapper;
import dto.FileGetDto;
import dto.FileUploadDto;
import entity.FileEntity;

public class FileMapper {

  // DTO -> ENTITY
  public static FileEntity toEntity(FileUploadDto dto) {
    return new FileEntity(
        dto.name(),
        dto.size(),
        dto.path(),
        null,
        dto.tags(),
        dto.type(),
        null,
        null
    );
  }

  // ENTITY -> DTO
  public static FileGetDto toDto(FileEntity entity) {
    return new FileGetDto(
        entity.getName(),
        entity.getKey(),
        entity.getSize(),
        entity.getUrl(),
        entity.getTags(),
        entity.getBucketName()
    );
  }
}