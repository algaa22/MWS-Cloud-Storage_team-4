package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;

import java.util.Map;

public interface FileContentRepository {
  String startMultipartUpload(String s3Key);

  String uploadPart(String uploadId, String s3Key, int partNum, byte[] bytes);

  byte[] downloadFilePart(String s3Key, long offset, long actualChunkSize);

  void completeMultipartUpload(String s3Key, String uploadId, Map<Integer, String> eTags);

  void putObject(String s3Key, byte[] data, String mimeType);

  byte[] downloadFile(String storagePath);

  void hardDeleteFile(String s3Key);

  void moveFile(FileEntity entity, String oldS3Key);
}
