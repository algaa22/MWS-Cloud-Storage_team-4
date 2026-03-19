package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mipt.team4.cloud_storage_backend.config.props.StorageConfig;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Repository
public class S3ContentRepository implements FileContentRepository {

  private static final Multimap<String, String> EMPTY_MAP = ImmutableMultimap.of();
  private final StorageConfig storageConfig;
  private final S3Wrapper wrapper;
  private final String bucketName;
  private final String region;

  private final S3Client s3Client;

  @Autowired
  public S3ContentRepository(StorageConfig storageConfig, S3Wrapper wrapper, S3Client s3Client) {
    this.storageConfig = storageConfig;
    this.wrapper = wrapper;

    this.bucketName = storageConfig.s3().userDataBucket().name();
    this.region = storageConfig.s3().region();
    this.s3Client = s3Client;
  }

  private String getBucketName() {
    return storageConfig.s3().userDataBucket().name();
  }

  @PostConstruct
  public void initialize() {
    if (!bucketExists(bucketName)) {
      createBucket(bucketName);
    }
  }

  @Override
  public void createBucket(String bucketName) {
    wrapper.execute(
        () -> {
          s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
          return null;
        });
  }

  @Override
  public boolean bucketExists(String bucketName) {
    return wrapper.execute(
        () -> {
          try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
            return true;
          } catch (S3Exception e) {
            if (e.statusCode() == 404 || e.statusCode() == 403) {
              return false;
            }
            throw e;
          }
        });
  }

  @Override
  public String startMultipartUpload(String s3Key) {
    return wrapper.execute(
        () ->
            s3Client
                .createMultipartUpload(
                    CreateMultipartUploadRequest.builder()
                        .bucket(getBucketName())
                        .key(s3Key)
                        .build())
                .uploadId());
  }

  @Override
  public String uploadPart(
      String uploadId, String s3Key, int partNum, InputStream inputStream, long size) {
    return wrapper.execute(
        () -> {
          UploadPartRequest uploadPartRequest =
              UploadPartRequest.builder()
                  .bucket(getBucketName())
                  .key(s3Key)
                  .uploadId(uploadId)
                  .partNumber(partNum)
                  .build();

          UploadPartResponse response =
              s3Client.uploadPart(
                  uploadPartRequest, RequestBody.fromInputStream(inputStream, size));

          return response.eTag();
        });
  }

  @Override
  public void completeMultipartUpload(String s3Key, String uploadId, Map<Integer, String> eTags) {
    wrapper.execute(
        () -> {
          List<CompletedPart> parts =
              eTags.entrySet().stream()
                  .map(
                      entry ->
                          CompletedPart.builder()
                              .partNumber(entry.getKey())
                              .eTag(entry.getValue())
                              .build())
                  .sorted(Comparator.comparing(CompletedPart::partNumber))
                  .toList();

          CompletedMultipartUpload completedUpload =
              CompletedMultipartUpload.builder().parts(parts).build();

          s3Client.completeMultipartUpload(
              CompleteMultipartUploadRequest.builder()
                  .bucket(getBucketName())
                  .key(s3Key)
                  .uploadId(uploadId)
                  .multipartUpload(completedUpload)
                  .build());
          return null;
        });
  }

  @Override
  public void putObject(String s3Key, byte[] data) {
    wrapper.execute(
        () -> {
          s3Client.putObject(
              PutObjectRequest.builder().bucket(getBucketName()).key(s3Key).build(),
              RequestBody.fromBytes(data));
          return null;
        });
  }

  @Override
  public InputStream downloadObject(String s3Key) {
    return wrapper.execute(
        () ->
            s3Client.getObject(
                GetObjectRequest.builder().bucket(getBucketName()).key(s3Key).build()));
  }

  @Override
  public boolean objectExists(String s3Key) {
    return wrapper.execute(
        () -> {
          try {
            s3Client.headObject(
                HeadObjectRequest.builder().bucket(getBucketName()).key(s3Key).build());
            return true;
          } catch (NoSuchKeyException e) {
            return false;
          }
        });
  }

  @Override
  public void hardDelete(String s3Key) {
    wrapper.execute(
        () -> {
          s3Client.deleteObject(
              DeleteObjectRequest.builder().bucket(getBucketName()).key(s3Key).build());
          return null;
        });
  }
}
