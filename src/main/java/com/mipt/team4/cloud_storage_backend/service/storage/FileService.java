package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.exception.database.StorageIllegalAccessException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.FileMapper;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.*;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.FileRepository;
import com.mipt.team4.cloud_storage_backend.service.user.UserSessionService;
import com.mipt.team4.cloud_storage_backend.utils.validation.StoragePaths;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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

  public void startChunkedUploadSession(FileChunkedUploadDto uploadSession)
      throws UserNotFoundException, StorageFileAlreadyExistsException {
    // TODO: разделить session'ы на юзеровский и файловский
    UUID userId = userSessionService.extractUserIdFromToken(uploadSession.userToken());
    String sessionId = uploadSession.sessionId();
    String path = uploadSession.path();
    if (activeUploads.containsKey(sessionId)) {
      throw new StorageFileAlreadyExistsException(userId, path);
    }

    String s3Key = StoragePaths.getS3Key(userId, path);
    Optional<FileEntity> fileEntity = fileRepository.getFile(userId, s3Key);
    if (fileEntity.isPresent()) throw new StorageFileAlreadyExistsException(userId, path);
    activeUploads.put(
        uploadSession.sessionId(), new ChunkedUploadState(uploadSession, userId, s3Key));
  }

  public void uploadChunk(UploadChunkDto chunk) throws UserNotFoundException {
    ChunkedUploadState upload = activeUploads.get(chunk.sessionId());
    if (upload == null) {
      throw new RuntimeException("Upload session not found!");
    }
    CompletableFuture<String> uploadId = upload.getOrCreateUploadId(fileRepository);
    int partNum = chunk.chunkIndex() + 1;
    if (chunk.chunkData().length > 10 * 1024 * 1024) {
      throw new RuntimeException("Chunk size exceeds maximum allowed size");
    }
    CompletableFuture<String> etag =
        fileRepository.uploadPart(uploadId, upload.s3Key, partNum, chunk.chunkData());
    upload.eTags.put(partNum, etag);

    upload.totalChunks++;
    upload.fileSize += chunk.chunkData().length;
  }

  public ChunkedUploadFileResultDto completeChunkedUpload(String sessionId)
      throws StorageFileAlreadyExistsException, UserNotFoundException {
    ChunkedUploadState upload = activeUploads.remove(sessionId);
    if (upload == null) throw new RuntimeException("No such upload session!");

    FileChunkedUploadDto session = upload.session;
    for (int i = 1; i <= upload.totalChunks; i++) {
      if (!upload.eTags.containsKey(i)) throw new RuntimeException("Missing chunk #" + i);
    }

    UUID fileId = UUID.randomUUID();
    UUID userId = userSessionService.extractUserIdFromToken(session.userToken());

    String s3Key = StoragePaths.getS3Key(userId, session.path());

    FileEntity fileEntity =
        new FileEntity(
            fileId,
            userId, // TODO: get actualUserId
            s3Key,
            guessMimeType(session.path()),
            "private",
            upload.fileSize,
            false,
            session.tags());

    fileRepository.completeMultipartUpload(fileEntity, upload.uploadId, upload.eTags);

    return new ChunkedUploadFileResultDto(session.path(), upload.fileSize, upload.totalChunks);
  }

  public void uploadFile(FileUploadDto fileUploadRequest)
      throws StorageFileAlreadyExistsException, UserNotFoundException {
    UUID fileId = UUID.randomUUID();
    UUID userId = userSessionService.extractUserIdFromToken(fileUploadRequest.userToken());

    String s3Key = StoragePaths.getS3Key(userId, fileUploadRequest.path());

    if (fileRepository.fileExists(userId, s3Key))
      throw new StorageFileAlreadyExistsException(userId, fileUploadRequest.path());

    String mimeType = guessMimeType(fileUploadRequest.path());
    byte[] data = fileUploadRequest.data();

    FileEntity entity =
        new FileEntity(
            fileId,
            userId,
            s3Key,
            mimeType,
            "private",
            data.length,
            false,
            fileUploadRequest.tags());

    // TODO: в FileEntity хранятся и s3Key, и обычный path
    fileRepository.addFile(entity, data);
  }

  public FileDownloadDto downloadFile(SimpleFileOperationDto fileDownload)
      throws StorageIllegalAccessException,
          UserNotFoundException,
          FileNotFoundException,
          StorageFileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(fileDownload.userToken());

    String s3Key = StoragePaths.getS3Key(userId, fileDownload.path());
    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, s3Key);

    FileEntity entity = entityOpt.orElseThrow(() -> new StorageFileNotFoundException(s3Key));

    return new FileDownloadDto(
        fileDownload.path(),
        entityOpt.get().getMimeType(),
        fileRepository.downloadFile(entity.getS3Key()));
  }

  public void deleteFile(SimpleFileOperationDto deleteFileRequest)
      throws StorageIllegalAccessException,
          UserNotFoundException,
          StorageFileNotFoundException,
          FileNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(deleteFileRequest.userToken());
    String s3Key = StoragePaths.getS3Key(userId, deleteFileRequest.path());

    fileRepository.deleteFile(userId, s3Key);
  }

  public DownloadedChunkDto getFileChunk(GetFileChunkDto fileChunkRequest)
      throws UserNotFoundException, StorageFileNotFoundException, StorageIllegalAccessException {
    UUID userId = userSessionService.extractUserIdFromToken(fileChunkRequest.userToken());
    String s3Key = StoragePaths.getS3Key(userId, fileChunkRequest.filePath());
    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, s3Key);

    FileEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileChunkRequest.filePath()));

    byte[] chunkData = fileRepository.downloadFilePart(entity.getS3Key());

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
    String s3Key = StoragePaths.getS3Key(userUuid, fileInfoRequest.path());
    Optional<FileEntity> entityOpt = fileRepository.getFile(userUuid, s3Key);
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
    String oldS3Key = StoragePaths.getS3Key(userId, changeFileMetadata.oldPath());

    Optional<FileEntity> entityOpt = fileRepository.getFile(userId, oldS3Key);

    FileEntity entity =
        entityOpt.orElseThrow(() -> new StorageFileNotFoundException(changeFileMetadata.oldPath()));

    if (changeFileMetadata.newPath().isPresent()) {
      String newS3Key = StoragePaths.getS3Key(userId, changeFileMetadata.newPath().get());

      Optional<FileEntity> existingFile = fileRepository.getFile(userId, newS3Key);
      if (existingFile.isPresent()) {
        throw new StorageFileAlreadyExistsException(
            userId, changeFileMetadata.newPath().get());
      }

      entity.setS3Key(StoragePaths.getS3Key(userId, changeFileMetadata.newPath().get()));
    }

    if (changeFileMetadata.tags().isPresent()) {
      entity.setTags(changeFileMetadata.tags().get());
    }

    if (changeFileMetadata.visibility().isPresent()) {
      entity.setVisibility(changeFileMetadata.visibility().get());
    }

    fileRepository.updateFile(entity, oldS3Key);
  }

  // TODO: хз как это сделать лучше
  public void createFolder(SimpleFolderOperationDto createFolder) throws UserNotFoundException {
    UUID userId = userSessionService.extractUserIdFromToken(createFolder.userToken());
    String folderPath = createFolder.folderPath();
    System.out.println("User " + userId + " created folder: " + folderPath);
  }


  public void changeFolderPath(ChangeFolderPathDto changeFolder)
      throws UserNotFoundException {

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
    List<String> filesInOldFolder = allFilePaths.stream()
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
        FileEntity file = fileOpt.get();
        try {
          byte[] fileContent = fileRepository.downloadFile(file.getS3Key());
          FileEntity newFileEntity = new FileEntity(
              UUID.randomUUID(),
              userId,
              newFilePath,
              file.getMimeType(),
              file.getVisibility(),
              file.getSize(),
              file.isDeleted(),
              file.getTags()
          );
          fileRepository.addFile(newFileEntity, fileContent);
          fileRepository.deleteFile(userId, oldFilePath);

        } catch (Exception e) {
          throw new RuntimeException("Failed to move file: " + oldFilePath, e);
        }
      }
    }
  }

  public void deleteFolder(SimpleFolderOperationDto request)
      throws UserNotFoundException {

    UUID userId = userSessionService.extractUserIdFromToken(request.userToken());
    String folderPath = request.folderPath();

    if (folderPath == null || folderPath.isEmpty()) {
      throw new RuntimeException("Folder path cannot be empty");
    }
    List<String> allFilePaths = fileRepository.getFilePathsList(userId);
    List<String> filesInFolder = allFilePaths.stream()
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

  private static class ChunkedUploadState {
    final FileChunkedUploadDto session;
    final Map<Integer, CompletableFuture<String>> eTags = new HashMap<>();
    final UUID userId;
    final String s3Key;

    CompletableFuture<String> uploadId;
    int fileSize = 0;
    int totalChunks = 0;

    // TODO: читаемость пупупу

    ChunkedUploadState(FileChunkedUploadDto session, UUID userId, String s3Key) {
      this.session = session;
      this.userId = userId;
      this.s3Key = s3Key;
    }

    CompletableFuture<String> getOrCreateUploadId(FileRepository repo) {
      if (uploadId == null) {
        String s3Key = StoragePaths.getS3Key(userId, session.path());
        uploadId = repo.startMultipartUpload(s3Key);
      }
      return uploadId;
    }
  }
}
