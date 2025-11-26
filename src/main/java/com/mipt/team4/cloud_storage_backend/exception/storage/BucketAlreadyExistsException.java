package com.mipt.team4.cloud_storage_backend.exception.storage;


public class BucketAlreadyExistsException extends Exception {
  public BucketAlreadyExistsException(String bucketName) {
    super("Bucket with name " + bucketName + " already exists");
  }
}
