package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.exception.storage.RecoverableStorageException;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MinioWrapper {

  public <T> T execute(MinioOperation<T> operation) {
    try {
      return operation.perform();
    } catch (Exception e) {
      throw classifyException(e);
    }
  }

  private RuntimeException classifyException(Exception e) {
    if (e instanceof ExecutionException || e instanceof java.util.concurrent.CompletionException) {
      Throwable cause = e.getCause();
      if (cause instanceof Exception) {
        return classifyException((Exception) cause);
      }
    }

    if (e instanceof InterruptedException) {
      Thread.currentThread().interrupt();
      return new FatalStorageException("Thread was interrupted", e);
    }

    if (isRecoverable(e)) {
      return new RecoverableStorageException("Temporary MinIO issue", e);
    }

    return new FatalStorageException("Critical MinIO error", e);
  }

  private boolean isRecoverable(Exception e) {
    if (e instanceof IOException) {
      return true;
    }

    if (e instanceof InternalException) {
      return true;
    }

    if (e instanceof InsufficientDataException) {
      return true;
    }

    if (e instanceof ErrorResponseException minioEx) {

      int httpStatus = minioEx.response().code();

      return httpStatus >= 500 || httpStatus == 429;
    }

    return false;
  }
}
