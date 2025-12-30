package com.mipt.team4.cloud_storage_backend.repository.storage;

import java.io.InputStream;
import java.util.Map;

public interface FileContentRepository {
  String startMultipartUpload(String s3Key);

  String uploadPart(String uploadId, String s3Key, int partNum, byte[] bytes);

  void completeMultipartUpload(String s3Key, String uploadId, Map<Integer, String> eTags);

  void putObject(String s3Key, byte[] data);

  InputStream downloadObject(String s3key);

  boolean objectExists(String s3Key);

  void hardDeleteFile(String s3Key);
}
