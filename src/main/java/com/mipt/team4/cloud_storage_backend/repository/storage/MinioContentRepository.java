package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mipt.team4.cloud_storage_backend.config.props.S3Config;
import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageObjectNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.messages.Part;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class MinioContentRepository implements FileContentRepository {

  private static final Multimap<String, String> EMPTY_MAP = ImmutableMultimap.of();
  private final S3Config s3Config;
  private final MinioWrapper wrapper;
  private final String bucketName;
  private final String region;

  private MinioAsyncClient minioClient;

  public MinioContentRepository(S3Config s3Config, MinioWrapper wrapper) {
    this.s3Config = s3Config;
    this.wrapper = wrapper;

    this.bucketName = s3Config.userDataBucket().name();
    this.region = s3Config.region();
  }

  @PostConstruct
  public void initialize() {
    try {
      minioClient =
          MinioAsyncClient.builder()
              .endpoint(s3Config.url())
              .credentials(s3Config.username(), s3Config.password())
              .build();
    } catch (Exception e) {
      throw new FatalStorageException("Failed to initialize MinIO", e);
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
  public String uploadPart(
      String uploadId, String s3Key, int partNum, InputStream inputStream, long size) {
    return wrapper
        .execute(
            () ->
                minioClient
                    .uploadPartAsync(
                        bucketName,
                        region,
                        s3Key,
                        inputStream,
                        size,
                        uploadId,
                        partNum,
                        EMPTY_MAP,
                        EMPTY_MAP)
                    .get())
        .etag();
  }

  @Override
  public void completeMultipartUpload(String s3Key, String uploadId, Map<Integer, String> eTags) {
    try {
      wrapper.execute(
          () -> {
            minioClient
                .completeMultipartUploadAsync(
                    bucketName,
                    region,
                    s3Key,
                    uploadId,
                    createPartArray(eTags),
                    EMPTY_MAP,
                    EMPTY_MAP)
                .get();
            return null;
          });
    } catch (UploadSessionNotFoundException exception) {
      if (!objectExists(s3Key)) {
        throw exception.getRecoverableCause();
      }
    }
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
    try {
      return wrapper.execute(
          () -> {
            minioClient
                .statObject(StatObjectArgs.builder().bucket(bucketName).object(s3Key).build())
                .get();
            return true;
          });
    } catch (StorageObjectNotFoundException e) {
      return false;
    }
  }

  @Override
  public void hardDelete(String s3Key) {
    wrapper.execute(
        () -> {
          if (objectExists(s3Key)) {
            minioClient
                .removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(s3Key).build())
                .get();
          }

          return null;
        });
  }

  private Part[] createPartArray(Map<Integer, String> eTags) {
    return eTags.entrySet().stream()
        .map(entry -> new Part(entry.getKey(), entry.getValue()))
        .sorted(Comparator.comparingInt(Part::partNumber))
        .toArray(Part[]::new);
  }
}
