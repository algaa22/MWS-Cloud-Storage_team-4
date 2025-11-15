package com.mipt.team4.cloud_storage_backend.exception.storage;

import java.util.UUID;

public class BucketAlreadyExistsException extends Exception {
  public BucketAlreadyExistsException(String bucketName) {
    super("Bucket with name " + bucketName + " already exists");
  }
}
