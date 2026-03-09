package com.mipt.team4.cloud_storage_backend.service.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.dto.requests.ChunkedUploadRequest;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.StorageEntity;
import com.mipt.team4.cloud_storage_backend.repository.storage.StorageRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

@Getter
public class ChunkedUploadState {

    private final List<byte[]> chunks = new ArrayList<>();
    // TODO: сессия не удаляется, если completeMultipartUpload не вызван
    // TODO: нужны ли все поля session?
    private final ChunkedUploadRequest request;
    private final Map<Integer, String> eTags = new HashMap<>();
    private final StorageEntity entity;
    private boolean stopped = false;
    private long fileSize = 0;
    private int totalParts = 0;
    private int partSize = 0;
    private int partNum = 0;
    private String uploadId;

    ChunkedUploadState(ChunkedUploadRequest request, StorageEntity entity) {
        this.request = request;
        this.entity = entity;
    }

    String getOrCreateUploadId(StorageRepository repo) {
        if (uploadId == null) {
            uploadId = repo.startMultipartUpload(entity);
        }

        return uploadId;
    }

    public void increaseTotalParts() {
        totalParts++;
    }

    public void addFileSize(long amount) {
        fileSize += amount;
    }

    public void addPartSize(int amount) {
        partSize += amount;
    }

    public void increasePartNum() {
        partNum++;
    }

    public void resetPartSize() {
        partSize = 0;
    }

    public void stop() {
        stopped = true;
    }

    public void resume() {
        stopped = false;
    }

    public void clear() {
        chunks.clear();
        eTags.clear();
    }
}
