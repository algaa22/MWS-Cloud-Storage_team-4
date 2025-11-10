package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.config.DatabaseConfig;
import com.mipt.team4.cloud_storage_backend.config.MinioConfig;
import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class MinioContentRepository implements FileContentRepository {
  private final MinioConfig config;
  private MinioClient minioClient;

  public MinioContentRepository(MinioConfig config) {
    this.config = config;
  }

  public void createBucket() {
      try {
          boolean bucketFound =
              minioClient.bucketExists(
                  BucketExistsArgs.builder()
                      .bucket("")
                      .build());
      } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
               InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
               XmlParserException e) {
          throw new RuntimeException(e);
      }
  }

  @Override
  public void initialize() {
    try {
      minioClient =
          MinioClient.builder()
              .endpoint(config.getUrl())
              .credentials(config.getUsername(), config.getPassword())
              .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String uploadPart(String uploadId, int partNum, byte[] bytes) {
    return "";
  }

  @Override
  public void completeMultipartUpload(String s3Key, String uploadId, List<String> etagList) {}

  @Override
  public void putObject(String s3Key, InputStream stream, String contentType) {}

  @Override
  public InputStream downloadObject(String storagePath) {
    return null;
  }

  @Override
  public String startMultipartUpload(String s3Key) {
    return "";
  }
}
