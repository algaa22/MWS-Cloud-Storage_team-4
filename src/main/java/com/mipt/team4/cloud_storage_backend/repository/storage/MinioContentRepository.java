package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mipt.team4.cloud_storage_backend.config.MinioConfig;
import com.mipt.team4.cloud_storage_backend.exception.storage.BucketAlreadyExistsException;
import com.mipt.team4.cloud_storage_backend.model.storage.entity.FileEntity;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class MinioContentRepository implements FileContentRepository {
  private MinioAsyncClient minioClient;

  public MinioContentRepository(String minioUrl) {
    initialize(minioUrl);
  }

  private void initialize(String minioUrl) {
    try {
      minioClient =
          MinioAsyncClient.builder()
              .endpoint(minioUrl)
              .credentials(MinioConfig.INSTANCE.getUsername(), MinioConfig.INSTANCE.getPassword())
              .build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    try {
      createBucket(MinioConfig.INSTANCE.getUserDataBucketName());
    } catch (BucketAlreadyExistsException _) {
    }
  }

  public void createBucket(String bucketName) throws BucketAlreadyExistsException {
    if (bucketExists(bucketName)) throw new BucketAlreadyExistsException(bucketName);

    try {
      minioClient.makeBucket(
          MakeBucketArgs.builder().bucket(MinioConfig.INSTANCE.getUserDataBucketName()).build());
    } catch (InsufficientDataException
        | InternalException
        | InvalidKeyException
        | IOException
        | NoSuchAlgorithmException
        | XmlParserException e) {
      throw new RuntimeException(e);
    }
  }

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
    // TODO: Think about headers...
    Multimap<String, String> headers = createEmptyHeader();
    Multimap<String, String> extraQueryParams = createEmptyHeader();

    CreateMultipartUploadResponse response;

    try {
      response =
          minioClient
              .createMultipartUploadAsync(
                  MinioConfig.INSTANCE.getUserDataBucketName(),
                  "eu-central-1",
                  s3Key,
                  headers,
                  extraQueryParams)
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

  // TODO: retry?

  @Override
  public String uploadPart(String uploadId, String s3Key, int partNum, byte[] bytes) {
    Multimap<String, String> extraHeaders = createEmptyHeader();
    Multimap<String, String> extraQueryParams = createEmptyHeader();
    InputStream inputStream = new ByteArrayInputStream(bytes);
    UploadPartResponse response;

    try {
      response =
          minioClient
              .uploadPartAsync(
                  MinioConfig.INSTANCE.getUserDataBucketName(),
                  "eu-central-1",
                  s3Key,
                  inputStream,
                  bytes.length,
                  uploadId,
                  partNum,
                  extraHeaders,
                  extraQueryParams)
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

  public byte[] downloadFilePart(String s3Key, long offset, long actualChunkSize) {
    return null;
  }

  public void updateFile(FileEntity entity) {

  }

  @Override
  public void completeMultipartUpload(String s3Key, String uploadId, Map<Integer, String> eTags) {
    Multimap<String, String> extraHeaders = createEmptyHeader();
    Multimap<String, String> extraQueryParams = createEmptyHeader();

    try {
      minioClient
          .completeMultipartUploadAsync(
              MinioConfig.INSTANCE.getUserDataBucketName(),
              "eu-central-1",
              s3Key,
              uploadId,
              createPartArray(eTags),
              extraHeaders,
              extraQueryParams)
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
  public void putObject(String s3Key, byte[] data, String mimeType) {
    InputStream stream = new ByteArrayInputStream(data);

    try {
      minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(MinioConfig.INSTANCE.getUserDataBucketName())
              .object(s3Key)
              .stream(stream, data.length, -1)
              .contentType(mimeType)
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

  @Override
  public byte[] downloadFile(String s3Key) {
    try {
      return minioClient
          .getObject(
              GetObjectArgs.builder()
                  .bucket(MinioConfig.INSTANCE.getUserDataBucketName())
                  .object(s3Key)
                  .build())
          .get()
          .readAllBytes();
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

  private Multimap<String, String> createEmptyHeader() {
    return MultimapBuilder.hashKeys().arrayListValues().build();
  }

  private Part[] createPartArray(Map<Integer, String> eTags)
      throws ExecutionException, InterruptedException {
    // TODO: объединять чанки произвольных размеров в части по 5МБ
    //       и, если непоследняя часть имеет размер < 5МБ, то бросать исключение
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
