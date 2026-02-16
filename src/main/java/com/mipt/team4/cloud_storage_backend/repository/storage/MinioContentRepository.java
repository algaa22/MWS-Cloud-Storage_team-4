package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mipt.team4.cloud_storage_backend.config.props.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.Part;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MinioContentRepository implements FileContentRepository {

  private static final Multimap<String, String> EMPTY_MAP = ImmutableMultimap.of();
  private final MinioWrapper wrapper = new MinioWrapper();
  private final String bucketName = MinioConfig.INSTANCE.getUserDataBucketName();
  private final String region = "eu-central-1";

  private final MinioConfig minioConfig;

  private MinioAsyncClient minioClient;

  @PostConstruct
  private void initialize() {
    try {
      minioClient =
          MinioAsyncClient.builder()
              .endpoint(minioConfig.url())
              .credentials(minioConfig.username(), minioConfig.password())
              .build();
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize MinIO", e);
    }

    if (!bucketExists(bucketName)) {
      createBucket(bucketName);
    }
  }

  @Override
  public void createBucket(String bucketName) {
    wrapper.execute(
        () -> {
          minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build()).get();
          return null;
        });
  }

  @Override
  public boolean bucketExists(String bucketName) {
    return wrapper.execute(
        () ->
            minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build()).get());
  }

  @Override
  public String startMultipartUpload(String s3Key) {
    return wrapper.execute(
        () ->
            minioClient
                .createMultipartUploadAsync(bucketName, region, s3Key, EMPTY_MAP, EMPTY_MAP)
                .get()
                .result()
                .uploadId());
  }

  @Override
  public String uploadPart(String uploadId, String s3Key, int partNum, byte[] bytes) {
    InputStream inputStream = new ByteArrayInputStream(bytes);
    return wrapper
        .execute(
            () ->
                minioClient
                    .uploadPartAsync(
                        bucketName,
                        region,
                        s3Key,
                        inputStream,
                        bytes.length,
                        uploadId,
                        partNum,
                        EMPTY_MAP,
                        EMPTY_MAP)
                    .get())
        .etag();
  }

  @Override
  public void completeMultipartUpload(String s3Key, String uploadId, Map<Integer, String> eTags) {

    wrapper.execute(
        () -> {
          minioClient
              .completeMultipartUploadAsync(
                  bucketName, region, s3Key, uploadId, createPartArray(eTags), EMPTY_MAP, EMPTY_MAP)
              .get();
          return null;
        });
  }

  @Override
  public void putObject(String s3Key, byte[] data) {
    InputStream stream = new ByteArrayInputStream(data);

    wrapper.execute(
        () -> {
          minioClient
              .putObject(
                  PutObjectArgs.builder().bucket(bucketName).object(s3Key).stream(
                          stream, data.length, -1)
                      .build())
              .get();
          return null;
        });
  }

  @Override
  public InputStream downloadObject(String s3Key) {
    return wrapper.execute(
        () ->
            minioClient
                .getObject(GetObjectArgs.builder().bucket(bucketName).object(s3Key).build())
                .get());
  }

  @Override
  public boolean objectExists(String s3Key) {
    return wrapper.execute(
        () -> {
          try {
            minioClient
                .statObject(StatObjectArgs.builder().bucket(bucketName).object(s3Key).build())
                .get();
            return true;
          } catch (Exception e) {
            Throwable cause = e instanceof ExecutionException ? e.getCause() : e;

            if (cause instanceof ErrorResponseException ex) {
              if ("NoSuchKey".equals(ex.errorResponse().code()) || ex.response().code() == 404) {
                return false;
              }
            }
            throw e;
          }
        });
  }

  @Override
  public void hardDeleteFile(String s3Key) {
    wrapper.execute(
        () -> {
          minioClient
              .removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(s3Key).build())
              .get();
          return null;
        });
  }

  private Part[] createPartArray(Map<Integer, String> eTags) {
    List<Part> partsList = new ArrayList<>(eTags.size());

    for (Map.Entry<Integer, String> entry : eTags.entrySet()) {
      int partNumber = entry.getKey();
      String eTag = entry.getValue();

      Part part = new Part(partNumber, eTag);
      partsList.add(part);
    }

    return partsList.toArray(Part[]::new);
  }
}
