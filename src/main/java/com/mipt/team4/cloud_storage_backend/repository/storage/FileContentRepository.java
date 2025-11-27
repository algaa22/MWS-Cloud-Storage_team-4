package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface FileContentRepository {
  CompletableFuture<String> startMultipartUpload(String s3Key);

  CompletableFuture<String> uploadPart(
      CompletableFuture<String> uploadId, String s3Key, int partNum, byte[] bytes);

  void completeMultipartUpload(
      String s3Key,
      CompletableFuture<String> uploadId,
      Map<Integer, CompletableFuture<String>> eTags);

  void putObject(String s3Key, byte[] data, String mimeType);

  byte[] downloadFile(String storagePath);

  void deleteFile(String s3Key);

  void moveFile(FileEntity entity);
}
