package service;

import dto.FileDto;
import entity.FileEntity;
import java.util.Optional;
import mapper.FileMapper;

import java.util.List;
import java.util.UUID;

public class FileService {
  //private final FileRepository repo;

  //public FileService(FileRepository repo) {
   // this.repo = repo;
  //}

  public FileDto createFile(FileDto dto) {
    FileEntity entity = FileMapper.toEntity(dto);
    //repo.save(entity);
    return FileMapper.toDto(entity);
  }

  public FileDto getFile(UUID id) {
    //return repo.findById(id).map(FileMapper::toDto).orElse(null);
  }

  public List<FileDto> getAllFiles() {
    //return repo.findAll().stream().map(FileMapper::toDto).toList();
  }

  public void deleteFile(UUID id) {
    //repo.deleteById(id);
  }

  public void rename(UUID id, String newName) {
    Optional<FileEntity> optEntity = repo.findById(fileId);
    FileEntity entity = optEntity.get();
    entity.setName(newName);
    // если путь включает имя файла, обнови path тоже:
    // entity.setPath(...);

    repo.save(entity); // перезаписываем файл с тем же id
    return FileMapper.toDto(entity);
  }
}

