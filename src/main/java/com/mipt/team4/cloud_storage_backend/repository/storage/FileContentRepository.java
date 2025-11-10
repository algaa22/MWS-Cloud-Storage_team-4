package com.mipt.team4.cloud_storage_backend.repository.storage;

import java.io.InputStream;
import java.util.List;

public interface FileContentRepository {
  public void initialize();

  String uploadPart(String uploadId, int partNum, byte[] bytes);

  void completeMultipartUpload(String s3Key, String uploadId, List<String> etagList);

  void putObject(String s3Key, InputStream stream, String contentType);

  InputStream downloadObject(String storagePath);

  String startMultipartUpload(String s3Key);
}
