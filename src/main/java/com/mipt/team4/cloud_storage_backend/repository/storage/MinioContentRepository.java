package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.google.common.collect.MultimapBuilder;
import com.mipt.team4.cloud_storage_backend.config.MinioConfig;
import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
import io.minio.*;
import io.minio.errors.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MinioContentRepository implements FileContentRepository {
  private MinioAsyncClient minioClient;

  public void createBucket() {
    try {
      CompletableFuture<Boolean> bucketFound = minioClient.bucketExists(BucketExistsArgs.builder().bucket("").build());
    } catch (InsufficientDataException
        | InternalException
        | InvalidKeyException
        | IOException
        | NoSuchAlgorithmException
        | XmlParserException e) {
      throw new RuntimeException(e);
    }

    try {
      minioClient.makeBucket(
          MakeBucketArgs.builder().bucket(StorageConfig.INSTANCE.getUserDataBucketName()).build());
    } catch (InsufficientDataException
        | InternalException
        | InvalidKeyException
        | IOException
        | NoSuchAlgorithmException
        | XmlParserException e) {
      // TODO: exceptions
      throw new RuntimeException(e);
    }
  }

  @Override
  public void initialize() {
    try {
      minioClient =
          MinioAsyncClient.builder()
              .endpoint(MinioConfig.INSTANCE.getUrl())
              .credentials(MinioConfig.INSTANCE.getUsername(), MinioConfig.INSTANCE.getPassword())
              .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String startMultipartUpload(String s3Key) {
    // TODO: return uploadId

    return "";
  }

  @Override
  public String uploadPart(String uploadId, int partNum, byte[] bytes) {
    // TODO: return eTag
    return "";
  }

  @Override
  public UUID completeMultipartUpload(
      String s3Key, String uploadId, Map<Integer, String> eTagList) {
    // TODO: return file id
    return null;
  }

  @Override
  public void putObject(String s3Key, InputStream stream, String contentType) {}

  @Override
  public InputStream downloadFile(String storagePath) {
    return null;
  }
}
