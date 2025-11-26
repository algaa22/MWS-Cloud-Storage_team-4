package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface FileContentRepository {
  String startMultipartUpload(String s3Key);

  String uploadPart(
      String uploadId, String s3Key, int partNum, byte[] bytes);

  void completeMultipartUpload(
      String s3Key,
      String uploadId,
      Map<Integer, String> eTags);

  void putObject(String s3Key, byte[] data, String mimeType);

  byte[] downloadFile(String storagePath);

  void deleteFile(String s3Key);

  void moveFile(FileEntity entity);
}
