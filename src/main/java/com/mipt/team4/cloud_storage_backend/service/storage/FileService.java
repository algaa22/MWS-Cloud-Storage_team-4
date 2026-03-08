package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.config.props.MinioConfig;
import com.mipt.team4.cloud_storage_backend.config.props.StorageNotificationConfig;
import com.mipt.team4.cloud_storage_backend.exception.retry.CompleteUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.retry.ProcessUploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.retry.UploadRetriableException;
import com.mipt.team4.cloud_storage_backend.exception.storage.MissingFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageFileNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.TooSmallFilePartException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadNotStoppedException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.user.UserNotFoundException;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.ChunkedUploadFileResult;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.StorageDto;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChangeFileMetadataRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileListFilter;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.FileUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.GetFileListRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SearchFilesByTagsRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.SimpleFileOperationRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UploadChunkRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.UploadPartRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.dto.responses.FileDownloadResponse;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileStatus;
import com.mipt.team4.cloud_storage_backend.model.user.entity.UserEntity;
import com.mipt.team4.cloud_storage_backend.notification.NotificationClient;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;
import com.mipt.team4.cloud_storage_backend.repository.user.UserRepository;
import com.mipt.team4.cloud_storage_backend.service.user.UserSessionService;
import com.mipt.team4.cloud_storage_backend.utils.ChunkCombiner;
import com.mipt.team4.cloud_storage_backend.utils.MimeTypeDetector;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    // TODO: нужен Scheduler для очистки старых сессий загрузки
    private final Map<String, ChunkedUploadState> activeUploads = new ConcurrentHashMap<>();

    private final UserSessionService userSessionService;
    private final StorageRepository storageRepository;
    private final UserRepository userRepository;
    private final MinioConfig minioConfig;
    private final NotificationClient notificationClient;
    private final StorageNotificationConfig storageNotificationConfig;

    public void startChunkedUploadSession(ChunkedUploadRequest request) {
        UUID userId = userSessionService.extractUserIdFromToken(request.userToken());
        UUID parentId = request.parentId().map(UUID::fromString).orElse(null);
        String uploadSessionId = request.sessionId();
        String name = request.name();

        if (activeUploads.containsKey(uploadSessionId)) {
            throw new StorageFileAlreadyExistsException(parentId, name);
        }

        Optional<StorageEntity> fileEntity = storageRepository.getFile(userId, parentId, name);
        if (fileEntity.isPresent()) {
            throw new StorageFileAlreadyExistsException(parentId, name);
        }

        activeUploads.put(
                request.sessionId(),
                new ChunkedUploadState(
                        request,
                        StorageEntity.builder()
                                .id(UUID.randomUUID())
                                .userId(userId)
                                .mimeType(MimeTypeDetector.detect(name))
                                .parentId(parentId)
                                .name(name)
                                .isDirectory(false)
                                .tags(request.tags())
                                .status(FileStatus.READY)
                                .updatedAt(LocalDateTime.now())
                                .build()));
    }

    public void resumeChunkedUploadSession(ChunkedUploadRequest request) {
        String uploadSessionId = request.sessionId();
        ChunkedUploadState uploadState = activeUploads.get(uploadSessionId);

        if (uploadState == null) {
            throw new UploadSessionNotFoundException();
        }

        if (!uploadState.isStopped()) {
            throw new UploadNotStoppedException();
        }

        uploadState.resume();
    }

    public void uploadChunk(UploadChunkRequest request) {
        ChunkedUploadState uploadState = activeUploads.get(request.sessionId());
        if (uploadState == null) {
            throw new UploadSessionNotFoundException();
        }

        uploadState.getChunks().add(request.chunkData());
        uploadState.addPartSize(request.chunkData().length);

        if (uploadState.getPartSize() >= minioConfig.minFilePartSize()) {
            uploadPart(uploadState);
        }
    }

    public ChunkedUploadFileResult completeChunkedUpload(String sessionId) {
        ChunkedUploadState uploadState = activeUploads.get(sessionId);
        if (uploadState == null) {
            throw new UploadSessionNotFoundException();
        }

        try {
            if (uploadState.getTotalParts() == 0) {
                throw new TooSmallFilePartException(minioConfig.minFilePartSize());
            }

            if (uploadState.getPartSize() != 0) {
                uploadPart(uploadState);
            }

            for (int i = 1; i <= uploadState.getTotalParts(); i++) {
                if (!uploadState.getETags().containsKey(i)) {
                    throw new MissingFilePartException(i);
                }
            }
        } catch (Exception exception) {
            if (!(exception instanceof UploadRetriableException)) {
                activeUploads.remove(sessionId);
            }
            throw exception;
        }

        StorageEntity fileEntity = uploadState.getEntity();

        try {
            storageRepository.completeMultipartUpload(
                    fileEntity, uploadState.getFileSize(), uploadState.getUploadId(), uploadState.getETags());
        } catch (UploadRetriableException exception) {
            uploadState.stop();
            throw new CompleteUploadRetriableException(exception.getCause());
        }
        checkStorageAndNotify(fileEntity.getUserId());

        userRepository.increaseUsedStorage(fileEntity.getUserId(), uploadState.getFileSize());
        activeUploads.remove(sessionId);

        return new ChunkedUploadFileResult(
                fileEntity.getId(), fileEntity.getSize(), uploadState.getTotalParts());
    }

    public UUID uploadFile(FileUploadRequest request) {
        UUID fileId = UUID.randomUUID();
        UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

        String fileName = request.name();
        UUID parentId = request.parentId().map(UUID::fromString).orElse(null);

        if (storageRepository.fileExists(userId, parentId, fileName)) {
            throw new StorageFileAlreadyExistsException(parentId, fileName);
        }

        String mimeType = MimeTypeDetector.detect(fileName);
        byte[] data = request.data();

        StorageEntity entity =
                StorageEntity.builder()
                        .id(fileId)
                        .userId(userId)
                        .mimeType(mimeType)
                        .size(data.length)
                        .parentId(parentId)
                        .name(fileName)
                        .isDirectory(false)
                        .tags(request.tags())
                        .status(FileStatus.READY)
                        .updatedAt(LocalDateTime.now())
                        .build();

        storageRepository.addFile(entity, data);
        userRepository.increaseUsedStorage(userId, data.length);

        checkStorageAndNotify(userId);
        return fileId;
    }

    public FileDownloadResponse downloadFile(SimpleFileOperationRequest request) {
        UUID fileId = UUID.fromString(request.fileId());
        UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

        Optional<StorageEntity> entityOpt = storageRepository.getFile(userId, fileId);
        StorageEntity entity = entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileId));

        return new FileDownloadResponse(
                entityOpt.get().getMimeType(), storageRepository.downloadFile(entity), entity.getSize());
    }

    public void deleteFile(SimpleFileOperationRequest request) {
        UUID fileId = UUID.fromString(request.fileId());
        UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

        Optional<StorageEntity> entityOpt = storageRepository.getFile(userId, fileId);
        StorageEntity entity = entityOpt.orElseThrow(() -> new StorageFileNotFoundException(fileId));

        storageRepository.deleteFile(entity);
        userRepository.decreaseUsedStorage(userId, entity.getSize());

        UserEntity user =
                userRepository
                        .getUserById(userId)
                        .orElseThrow(() -> new UserNotFoundException(request.userToken()));

        String filePath = storageRepository.getFullFilePath(entity.getId());
        if (filePath == null) {
            filePath = entity.getName();
            log.warn(
                    "Could not get full path for file {}, using name only: {}", entity.getId(), filePath);
        }

        notificationClient.notifyFileDeleted(user.getEmail(), user.getName(), filePath, userId);
        log.info("File deleted: {} (path: {})", entity.getId(), filePath);
    }

    public List<StorageEntity> getFileList(GetFileListRequest request) {
        UUID parentId = request.parentId().map(UUID::fromString).orElse(null);
        UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

        return storageRepository.getFileList(
                new FileListFilter(userId, parentId, request.includeDirectories(), request.recursive()));
    }

    public StorageDto getFileInfo(SimpleFileOperationRequest request) {
        UUID fileId = UUID.fromString(request.fileId());
        UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

        Optional<StorageEntity> entityOpt = storageRepository.getFile(userId, fileId);
        if (entityOpt.isEmpty()) {
            throw new StorageFileNotFoundException(fileId);
        }

        return new StorageDto(entityOpt.get());
    }

    public List<StorageEntity> searchFilesByTags(SearchFilesByTagsRequest request)
            throws UserNotFoundException {
        UUID userId = userSessionService.extractUserIdFromToken(request.userToken());
        return storageRepository.getFilesByTags(userId, request.tags());
    }

    public void changeFileMetadata(ChangeFileMetadataRequest request) {
        UUID fileId = UUID.fromString(request.fileId());
        UUID userId = userSessionService.extractUserIdFromToken(request.userToken());

        StorageEntity entity =
                storageRepository
                        .getFile(userId, fileId)
                        .filter(f -> f.getUserId().equals(userId))
                        .orElseThrow(() -> new StorageFileNotFoundException(fileId));

        String targetName = request.newName().orElse(entity.getName());
        UUID targetParentId =
                request.newParentId().isPresent()
                        ? UUID.fromString(request.newParentId().get())
                        : entity.getParentId();

        if (request.newName().isPresent() || request.newParentId().isPresent()) {
            if (storageRepository.fileExists(userId, targetParentId, targetName)) {
                throw new StorageFileAlreadyExistsException(targetParentId, targetName);
            }
            entity.setName(targetName);
            entity.setParentId(targetParentId);
        }

        request.tags().ifPresent(entity::setTags);
        request.visibility().ifPresent(entity::setVisibility);

        storageRepository.updateFile(entity);
    }

    private void uploadPart(ChunkedUploadState uploadState) {
        String uploadId = uploadState.getOrCreateUploadId(storageRepository);
        byte[] part = ChunkCombiner.combineChunksToPart(uploadState);
        StorageEntity entity = uploadState.getEntity();

        String eTag;

        try {
            eTag =
                    storageRepository.uploadPart(
                            entity,
                            new UploadPartRequest(
                                    uploadId, entity.getUserId(), entity.getId(), uploadState.getPartNum(), part));
        } catch (UploadRetriableException exception) {
            uploadState.stop();
            throw new ProcessUploadRetriableException(
                    uploadState.getFileSize(), uploadState.getPartNum(), exception.getCause());
        } finally {
            uploadState.getChunks().clear();
            uploadState.resetPartSize();
        }

        uploadState.getETags().put(uploadState.getPartNum(), eTag);
        uploadState.addFileSize(part.length);
        uploadState.increaseTotalParts();
    }

    private void checkStorageAndNotify(UUID userId) {
        userRepository
                .getStorageUsage(userId)
                .ifPresent(
                        usage -> {
                            double ratio = usage.getRatio();

                            log.info(
                                    "Storage check for user {}: used={}, limit={}, {}%",
                                    userId, usage.used(), usage.limit(), String.format("%.2f", ratio * 100));

                            if (ratio >= storageNotificationConfig.getFullThreshold()) {
                                userRepository
                                        .getUserById(userId)
                                        .ifPresent(
                                                user ->
                                                        notificationClient.notifyStorageFull(
                                                                user.getEmail(), user.getName(), userId));
                            } else if (ratio >= storageNotificationConfig.getAlmostFullThreshold()) {
                                userRepository
                                        .getUserById(userId)
                                        .ifPresent(
                                                user ->
                                                        notificationClient.notifyStorageAlmostFull(
                                                                user.getEmail(),
                                                                user.getName(),
                                                                usage.used(),
                                                                usage.limit(),
                                                                userId));
                            }
                        });
    }
}