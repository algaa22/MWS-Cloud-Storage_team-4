package com.mipt.team4.cloud_storage_backend.repository.storage;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface FileContentRepository {
  void initialize();

  CompletableFuture<String> startMultipartUpload(String s3Key);

  CompletableFuture<String> uploadPart(CompletableFuture<String> uploadId, String s3Key, int partNum, byte[] bytes);

  void completeMultipartUpload(
          String s3Key,
          CompletableFuture<String> uploadId,
          Map<Integer, CompletableFuture<String>> eTags);

  void putObject(String s3Key, InputStream stream, String contentType);

  InputStream downloadFile(String storagePath);
}
