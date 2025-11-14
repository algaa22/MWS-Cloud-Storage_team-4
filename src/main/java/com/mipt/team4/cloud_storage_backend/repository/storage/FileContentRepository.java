package com.mipt.team4.cloud_storage_backend.repository.storage;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

public interface FileContentRepository {
  void initialize();

  String uploadPart(String uploadId, int partNum, byte[] bytes);

  UUID completeMultipartUpload(String s3Key, String uploadId, Map<Integer, String> eTagList);

  void putObject(String s3Key, InputStream stream, String contentType);

  InputStream downloadFile(String storagePath);

  String startMultipartUpload(String s3Key);
}
