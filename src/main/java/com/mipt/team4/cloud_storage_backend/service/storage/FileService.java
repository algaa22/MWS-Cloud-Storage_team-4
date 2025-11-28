package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.CombineChunksToPartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooSmallFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.FileMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.*;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileRepository;
import com.mipt.team4.cloud_storage_backend.service.user.UserSessionService;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class FileService {
  private final FileRepository fileRepository;
  private final Map<String, ChunkedUploadState> activeUploads = new ConcurrentHashMap<>();
  private final UserSessionService userSessionService;

  public FileService(FileRepository fileRepository, UserSessionService userSessionService) {
    this.fileRepository = fileRepository;
    this.userSessionService = userSessionService;
  }

  // TODO: soft delete?

  public void startChunkedUploadSession(FileChunkedUploadDto uploadSession)
      throws UserNotFoundException, StorageFileAlreadyExistsException {
    // TODO: разделить session'ы на юзеровский и файловский
    UUID userId = userSessionService.extractUserIdFromToken(uploadSession.userToken());
    String sessionId = uploadSession.sessionId();
    String path = uploadSession.path();
    if (activeUploads.containsKey(sessionId)) {
      throw new StorageFileAlreadyExistsException(userId, path);
    }

    Optional<FileEntity> fileEntity = fileRepository.getFile(userId, path);
    if (fileEntity.isPresent()) throw new StorageFileAlreadyExistsException(userId, path);

    UUID newFileId = UUID.randomUUID();

    activeUploads.put(
        uploadSession.sessionId(), new ChunkedUploadState(uploadSession, userId, newFileId, path));
  }

  public void uploadChunk(UploadChunkDto uploadRequest)
      throws UserNotFoundException, CombineChunksToPartException {
    ChunkedUploadState uploadState = activeUploads.get(uploadRequest.sessionId());
    if (uploadState == null) {
      throw new RuntimeException("Upload session not found!");
    }

    uploadState.chunks.add(uploadRequest.chunkData());
    uploadState.partSize += uploadRequest.chunkData().length;

    if (uploadState.partSize >= 5 * 1024 * 1024) {
      uploadPart(uploadState);
    }
  }

  public ChunkedUploadFileResultDto completeChunkedUpload(String sessionId)
      throws StorageFileAlreadyExistsException,
          UserNotFoundException,
          TooSmallFilePartException,
          CombineChunksToPartException {
    ChunkedUploadState uploadState = activeUploads.remove(sessionId);
    if (uploadState == null) throw new RuntimeException("No such upload session!");

    if (uploadState.totalParts == 0) {
      throw new TooSmallFilePartException();
    }

    if (uploadState.partSize != 0) {
      uploadPart(uploadState);
    }

    FileChunkedUploadDto session = uploadState.session;
    for (int i = 1; i <= uploadState.totalParts; i++) {
      if (!uploadState.eTags.containsKey(i)) throw new RuntimeException("Missing chunk #" + i);
    }

    UUID userId = userSessionService.extractUserIdFromToken(session.userToken());

    FileEntity fileEntity =
        new FileEntity(
            uploadState.fileId,
            userId, // TODO: get actualUserId
            uploadState.path,
            guessMimeType(session.path()),
            "private",
            uploadState.fileSize,
            false,
            session.tags());

    fileRepository.completeMultipartUpload(fileEntity, uploadState.uploadId, uploadState.eTags);

    return new ChunkedUploadFileResultDto(
        session.path(), uploadState.fileSize, uploadState.totalParts);
  }

  public void uploadFile(FileUploadDto fileUploadRequest)
      throws StorageFileAlreadyExistsException, UserNotFoundException {
    UUID fileId = UUID.randomUUID();
    UUID userId = userSessionService.extractUserIdFromToken(fileUploadRequest.userToken());

    if (fileRepository.fileExists(userId, fileUploadRequest.path()))
      throw new StorageFileAlreadyExistsException(userId, fileUploadRequest.path());

    String mimeType = guessMimeType(fileUploadRequest.path());
    byte[] data = fileUploadRequest.data();

    FileEntity entity =
        new FileEntity(
            fileId,
            userId,
            fileUploadRequest.path(),
            mimeType,
            "private",
            data.length,
            false,
            fileUploadRequest.tags());

    // TODO: в FileEntity хранятся и s3Key, и обычный path
    fileRepository.addFile(entity, data);
  }

  public FileDownloadDto downloadFile(SimpleFileOperationDto fileDownload)
      throws UserNotFoundException, StorageFileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(fileDownload.userToken());

    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, fileDownload.path());
    FileEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileDownload.path()));

    return new FileDownloadDto(
        fileDownload.path(), entityOpt.get().getMimeType(), fileRepository.downloadFile(entity));
  }

  public void deleteFile(SimpleFileOperationDto deleteFileRequest)
      throws UserNotFoundException, StorageFileNotFoundException, FileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(deleteFileRequest.userToken());
    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, deleteFileRequest.path());
    FileEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(deleteFileRequest.path()));

    fileRepository.deleteFile(entity);
  }

  public DownloadedChunkDto getFileChunk(GetFileChunkDto fileChunkRequest)
      throws UserNotFoundException, StorageFileNotFoundException, StorageIllegalAccessException {
    UUID userId = userSessionService.extractUserIdFromToken(fileChunkRequest.userToken());
    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, fileChunkRequest.filePath());

    FileEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileChunkRequest.filePath()));

    long chunkSize = fileChunkRequest.chunkSize();
    long offset = fileChunkRequest.chunkIndex() * chunkSize;
    byte[] chunkData =
        fileRepository.downloadFilePart(entity.getOwnerId(), entity.getFileId(), offset, chunkSize);

    return new DownloadedChunkDto(
        fileChunkRequest.filePath(), fileChunkRequest.chunkIndex(), chunkData);
  }

  public List<String> getFilePathsList(GetFilePathsListDto filePathsRequest)
      throws UserNotFoundException {
    UUID userUuid = userSessionService.extractUserIdFromToken(filePathsRequest.userToken());
    return fileRepository.getFilePathsList(userUuid);
  }

  public FileDto getFileInfo(SimpleFileOperationDto fileInfoRequest)
      throws UserNotFoundException, StorageFileNotFoundException {
    UUID userUuid = userSessionService.extractUserIdFromToken(fileInfoRequest.userToken());

    Optional<FileEntity> entityOpt = fileRepository.getFile(userUuid, fileInfoRequest.path());
    if (entityOpt.isEmpty()) throw new StorageFileNotFoundException(fileInfoRequest.path());

    return FileMapper.toDto(entityOpt.get());
  }

  public FileChunkedDownloadDto getFileDownloadInfo(SimpleFileOperationDto fileInfo)
      throws UserNotFoundException, StorageFileNotFoundException, StorageIllegalAccessException {
    UUID userUuid = userSessionService.extractUserIdFromToken(fileInfo.userToken());
    Optional<FileEntity> entityOpt = fileRepository.getFile(userUuid, fileInfo.path());
    FileEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileInfo.path()));

    return new FileChunkedDownloadDto(
        entity.getFileId(), fileInfo.path(), entity.getMimeType(), entity.getSize());
  }

  public void changeFileMetadata(ChangeFileMetadataDto changeFileMetadata)
      throws UserNotFoundException,
          StorageFileNotFoundException,
          StorageFileAlreadyExistsException {

    UUID userId = userSessionService.extractUserIdFromToken(changeFileMetadata.userToken());

    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, changeFileMetadata.oldPath());

    FileEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(changeFileMetadata.oldPath()));

    if (changeFileMetadata.newPath().isPresent()) {
      Optional<FileEntity> existingFile =
          fileRepository.getFile(userId, changeFileMetadata.newPath().get());
      if (existingFile.isPresent()) {
        throw new StorageFileAlreadyExistsException(userId, changeFileMetadata.newPath().get());
      }

      entity.setPath(changeFileMetadata.newPath().get());
    }

    if (changeFileMetadata.tags().isPresent()) {
      entity.setTags(changeFileMetadata.tags().get());
    }

    if (changeFileMetadata.visibility().isPresent()) {
      entity.setVisibility(changeFileMetadata.visibility().get());
    }

    fileRepository.updateFile(entity);
  }

  // TODO: хз как это сделать лучше

  public void createFolder(SimpleFolderOperationDto createFolder) throws UserNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(createFolder.userToken());
    String folderPath = createFolder.folderPath();
    System.out.println("User " + userId + " created folder: " + folderPath);
  }

  public void changeFolderPath(ChangeFolderPathDto changeFolder) throws UserNotFoundException {

    UUID userId = userSessionService.extractUserIdFromToken(changeFolder.userToken());
    String oldPath = changeFolder.oldFolderPath();
    String newPath = changeFolder.newFolderPath();
    if (oldPath == null || oldPath.isEmpty() || newPath == null || newPath.isEmpty()) {
      throw new RuntimeException("Folder paths cannot be empty");
    }
    if (oldPath.equals(newPath)) {
      throw new RuntimeException("Old and new paths are the same");
    }
    List<String> allFilePaths = fileRepository.getFilePathsList(userId);
    List<String> filesInOldFolder =
        allFilePaths.stream()
            .filter(filePath -> filePath.startsWith(oldPath + "/"))
            .collect(Collectors.toList());
    if (filesInOldFolder.isEmpty()) {
      throw new RuntimeException("Folder not found or empty: " + oldPath);
    }
    for (String oldFilePath : filesInOldFolder) {
      String newFilePath = oldFilePath.replaceFirst(oldPath, newPath);
      if (fileRepository.fileExists(userId, newFilePath)) {
        throw new RuntimeException("File already exists at new path: " + newFilePath);
      }
      Optional<FileEntity> fileOpt = fileRepository.getFile(userId, oldFilePath);
      if (fileOpt.isPresent()) {
        FileEntity fileEntity = fileOpt.get();
        try {
          byte[] fileContent = fileRepository.downloadFile(fileEntity);
          FileEntity newFileEntity =
              new FileEntity(
                  UUID.randomUUID(),
                  userId,
                  newFilePath,
                  fileEntity.getMimeType(),
                  fileEntity.getVisibility(),
                  fileEntity.getSize(),
                  fileEntity.isDeleted(),
                  fileEntity.getTags());
          fileRepository.addFile(newFileEntity, fileContent);
          fileRepository.deleteFile(userId, oldFilePath);

        } catch (Exception e) {
          throw new RuntimeException("Failed to move file: " + oldFilePath, e);
        }
      }
    }
  }

  public void deleteFolder(SimpleFolderOperationDto request) throws UserNotFoundException {

    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());
    String folderPath = request.folderPath();

    if (folderPath == null || folderPath.isEmpty()) {
      throw new RuntimeException("Folder path cannot be empty");
    }
    List<String> allFilePaths = fileRepository.getFilePathsList(userId);
    List<String> filesInFolder =
        allFilePaths.stream()
            .filter(filePath -> filePath.startsWith(folderPath + "/"))
            .collect(Collectors.toList());
    if (filesInFolder.isEmpty()) {
      throw new RuntimeException("Folder not found or empty: " + folderPath);
    }
    for (String filePath : filesInFolder) {
      try {
        fileRepository.deleteFile(userId, filePath);
      } catch (Exception e) {
        throw new RuntimeException("Failed to delete file: " + filePath, e);
      }
    }
  }

  private void uploadPart(ChunkedUploadState uploadState) throws CombineChunksToPartException {
    // TODO: ay ay ay... hardcoding
    byte[] part = combineChunksToPart(uploadState);

    String uploadId = uploadState.getOrCreateUploadId(fileRepository);
    if (part.length > 10 * 1024 * 1024) {
      // TODO: ne tak
      throw new RuntimeException("Chunk size exceeds maximum allowed size");
    }

    String eTag =
        fileRepository.uploadPart(
            uploadId, uploadState.userId, uploadState.fileId, uploadState.partNum, part);
    uploadState.eTags.put(uploadState.partNum, eTag);

    uploadState.totalParts++;
    uploadState.fileSize += part.length;
  }

  // TODO: в другой класс?
  private byte[] combineChunksToPart(ChunkedUploadState upload)
      throws CombineChunksToPartException {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      for (byte[] chunk : upload.chunks) {
        outputStream.write(chunk);
      }

      upload.chunks.clear();
      upload.partSize = 0;
      upload.partNum++;

      return outputStream.toByteArray();
    } catch (IOException e) {
      throw new CombineChunksToPartException();
    }
  }

  private static class ChunkedUploadState {

    // TODO: сессия не удаляется, если completeMultipartUpload не вызван
    final FileChunkedUploadDto session;
    final Map<Integer, String> eTags = new HashMap<>();
    final List<byte[]> chunks = new ArrayList<>();
    final UUID userId;
    final UUID fileId;
    final String path;

    String uploadId;
    int fileSize = 0;
    int totalParts = 0;
    int partSize = 0;
    int partNum = 0;

    // TODO: читаемость пупупу

    ChunkedUploadState(FileChunkedUploadDto session, UUID userId, UUID fileId, String path) {
      this.session = session;
      this.userId = userId;
      this.fileId = fileId;
      this.path = path;
    }

    String getOrCreateUploadId(FileRepository repo) {
      if (uploadId == null) {
        uploadId = repo.startMultipartUpload(userId, fileId);
      }

      return uploadId;
    }
  }

  private String guessMimeType(String filePath) {
    // TODO: вынести в отдельный класс, добавить типов файлов
    if (filePath == null) return "application/octet-stream";
    if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) return "image/jpeg";
    if (filePath.endsWith(".png")) return "image/png";
    if (filePath.endsWith(".gif")) return "image/gif";
    if (filePath.endsWith(".pdf")) return "application/pdf";
    if (filePath.endsWith(".txt")) return "text/plain";
    if (filePath.endsWith(".html")) return "text/html";
    if (filePath.endsWith(".mp3")) return "audio/mpeg";
    if (filePath.endsWith(".mp4")) return "video/mp4";
    return "application/octet-stream";
  }
}
