package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.mipt.team4.cloud_storage_backend.config.props.MinioConfig;
import io.minio.BucketExistsArgs;
import io.minio.CreateMultipartUploadResponse;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.UploadPartResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.XmlParserException;
import io.minio.messages.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Repository;

@Repository
public class MinioContentRepository implements FileContentRepository {

  private static final Multimap<String, String> EMPTY_MAP = ImmutableMultimap.of();

  private final MinioConfig minioConfig;

  private MinioAsyncClient minioClient;

  public MinioContentRepository(MinioConfig minioConfig) {
    this.minioConfig = minioConfig;

    initialize(minioConfig.url());
  }

  private void initialize(String minioUrl) {
    try {
      minioClient =
          MinioAsyncClient.builder()
              .endpoint(minioUrl)
              .credentials(minioConfig.username(), minioConfig.password())
              .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    createBucket(minioConfig.userDataBucket().name());
  }

  @Override
  public void createBucket(String bucketName) {
    try {
      minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
    } catch (InsufficientDataException
        | InternalException
        | InvalidKeyException
        | IOException
        | NoSuchAlgorithmException
        | XmlParserException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean bucketExists(String bucketName) {
    try {
      return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build()).get();
    } catch (InsufficientDataException
        | InternalException
        | InvalidKeyException
        | IOException
        | NoSuchAlgorithmException
        | XmlParserException
        | ExecutionException
        | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String startMultipartUpload(String s3Key) {
    CreateMultipartUploadResponse response;

    try {
      response =
          minioClient
              .createMultipartUploadAsync(
                  minioConfig.userDataBucket().name(), "eu-central-1", s3Key, EMPTY_MAP, EMPTY_MAP)
              .get();
    } catch (InsufficientDataException
        | InternalException
        | InvalidKeyException
        | IOException
        | NoSuchAlgorithmException
        | XmlParserException
        | ExecutionException
        | InterruptedException e) {
      throw new RuntimeException(e);
    }

    return response.result().uploadId();
  }

  @Override
  public String uploadPart(String uploadId, String s3Key, int partNum, byte[] bytes) {
    InputStream inputStream = new ByteArrayInputStream(bytes);
    UploadPartResponse response;

    try {
      response =
          minioClient
              .uploadPartAsync(
                  minioConfig.userDataBucket().name(),
                  "eu-central-1",
                  s3Key,
                  inputStream,
                  bytes.length,
                  uploadId,
                  partNum,
                  EMPTY_MAP,
                  EMPTY_MAP)
              .get();
    } catch (InterruptedException
        | XmlParserException
        | NoSuchAlgorithmException
        | IOException
        | InvalidKeyException
        | InternalException
        | InsufficientDataException
        | ExecutionException ex) {
      throw new RuntimeException(ex);
    }

    return response.etag();
  }

  @Override
  public void completeMultipartUpload(String s3Key, String uploadId, Map<Integer, String> eTags) {

    try {
      minioClient
          .completeMultipartUploadAsync(
              minioConfig.userDataBucket().name(),
              "eu-central-1",
              s3Key,
              uploadId,
              createPartArray(eTags),
              EMPTY_MAP,
              EMPTY_MAP)
          .get();
    } catch (InsufficientDataException
        | InternalException
        | InvalidKeyException
        | IOException
        | NoSuchAlgorithmException
        | XmlParserException
        | ExecutionException
        | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void putObject(String s3Key, byte[] data) {
    InputStream stream = new ByteArrayInputStream(data);

    try {
      minioClient
          .putObject(
              PutObjectArgs.builder()
                  .bucket(minioConfig.userDataBucket().name())
                  .object(s3Key)
                  .stream(stream, data.length, -1)
                  .build())
          .get();
    } catch (InsufficientDataException
        | XmlParserException
        | NoSuchAlgorithmException
        | IOException
        | InvalidKeyException
        | InternalException
        | ExecutionException
        | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public InputStream downloadObject(String s3Key) {
    try {
      return minioClient
          .getObject(
              GetObjectArgs.builder()
                  .bucket(minioConfig.userDataBucket().name())
                  .object(s3Key)
                  .build())
          .get();
    } catch (InsufficientDataException
        | XmlParserException
        | NoSuchAlgorithmException
        | IOException
        | InvalidKeyException
        | InternalException
        | InterruptedException
        | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean objectExists(String s3Key) {
    try {
      minioClient
          .statObject(
              StatObjectArgs.builder()
                  .bucket(minioConfig.userDataBucket().name())
                  .object(s3Key)
                  .build())
          .get();

      return true;
    } catch (ExecutionException e) {
      if (e.getCause().getCause() instanceof ErrorResponseException errorResponseException) {
        if (errorResponseException.errorResponse().code().equals("NoSuchKey")) {
          return false;
        }
      }

      throw new RuntimeException(e);
    } catch (InsufficientDataException
        | InternalException
        | InvalidKeyException
        | IOException
        | NoSuchAlgorithmException
        | XmlParserException
        | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void hardDeleteFile(String s3Key) {
    try {
      minioClient.removeObject(
          RemoveObjectArgs.builder()
              .bucket(minioConfig.userDataBucket().name())
              .object(s3Key)
              .build());
    } catch (InsufficientDataException
        | XmlParserException
        | NoSuchAlgorithmException
        | IOException
        | InvalidKeyException
        | InternalException e) {
      throw new RuntimeException(e);
    }
  }

  private Part[] createPartArray(Map<Integer, String> eTags)
      throws ExecutionException, InterruptedException {
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
