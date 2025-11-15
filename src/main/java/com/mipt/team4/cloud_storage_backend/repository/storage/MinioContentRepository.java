package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mipt.team4.cloud_storage_backend.config.MinioConfig;
import com.mipt.team4.cloud_storage_backend.config.StorageConfig;
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

  public void createBucket() {
    try {
      CompletableFuture<Boolean> bucketFound =
          minioClient.bucketExists(BucketExistsArgs.builder().bucket("").build());
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

  private Multimap<String, String> createEmptyHeader() {
    return MultimapBuilder.hashKeys().arrayListValues().build();
  }

  @Override
  public CompletableFuture<String> startMultipartUpload(String s3Key) {
    // TODO: Think about headers...
    Multimap<String, String> headers = createEmptyHeader();
    Multimap<String, String> extraQueryParams = createEmptyHeader();

    CompletableFuture<CreateMultipartUploadResponse> futureResponse;

    try {
      futureResponse =
          minioClient.createMultipartUploadAsync(
              StorageConfig.INSTANCE.getUserDataBucketName(),
              "eu-central-1",
              s3Key,
              headers,
              extraQueryParams);
    } catch (InsufficientDataException
        | InternalException
        | InvalidKeyException
        | IOException
        | NoSuchAlgorithmException
        | XmlParserException e) {
      throw new RuntimeException(e);
    }

    return futureResponse
        .thenApply(response -> response.result().uploadId())
        .exceptionally(
            e -> {
              // TODO
              return "";
            });
  }

  @Override
  public CompletableFuture<String> uploadPart(
      CompletableFuture<String> uploadId, String s3Key, int partNum, byte[] bytes) {
    Multimap<String, String> extraHeaders = createEmptyHeader();
    Multimap<String, String> extraQueryParams = createEmptyHeader();
    InputStream inputStream = new ByteArrayInputStream(bytes);

    CompletableFuture<UploadPartResponse> futureResponse =
        uploadId
            .thenApply(
                uploadIdStr -> {
                  try {
                    return minioClient
                        .uploadPartAsync(
                            StorageConfig.INSTANCE.getUserDataBucketName(),
                            "eu-central-1",
                            s3Key,
                            inputStream,
                            bytes.length,
                            uploadIdStr,
                            partNum,
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
                })
            .exceptionally(
                e -> {
                  // TODO
                  return null;
                });

    return futureResponse
        .thenApply(UploadPartResponse::etag)
        .exceptionally(
            e -> {
              // TODO
              return null;
            });
  }

  @Override
  public void completeMultipartUpload(
      String s3Key,
      CompletableFuture<String> uploadId,
      Map<Integer, CompletableFuture<String>> eTags) {
    Multimap<String, String> extraHeaders = createEmptyHeader();
    Multimap<String, String> extraQueryParams = createEmptyHeader();

    uploadId
        .thenAccept(
            uploadIdStr -> {
              try {
                minioClient
                    .completeMultipartUploadAsync(
                        StorageConfig.INSTANCE.getUserDataBucketName(),
                        "eu-central-1",
                        s3Key,
                        uploadIdStr,
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
            })
        .exceptionally(
            e -> {
              // TODO
              return null;
            });
  }

  private Part[] createPartArray(Map<Integer, CompletableFuture<String>> eTags) {
    List<Part> partsList = new ArrayList<>(eTags.size());

    for (Map.Entry<Integer, CompletableFuture<String>> entry : eTags.entrySet()) {
      int partNumber = entry.getKey();
      CompletableFuture<String> eTag = entry.getValue();

      eTag.thenAccept(eTagStr -> partsList.add(new Part(partNumber, eTagStr)));
    }

    return partsList.toArray(new Part[0]);
  }

  @Override
  public void putObject(String s3Key, byte[] data, String mimeType) {
    InputStream stream = new ByteArrayInputStream(data);

    try {
        minioClient.putObject(
          PutObjectArgs.builder()
              .bucket(StorageConfig.INSTANCE.getUserDataBucketName())
              .object(s3Key)
              .stream(stream, data.length, data.length)
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
  public InputStream downloadFile(String s3Key) {
    try {
      return minioClient
          .getObject(
              GetObjectArgs.builder()
                  .bucket(StorageConfig.INSTANCE.getUserDataBucketName())
                  .object(s3Key)
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
}
