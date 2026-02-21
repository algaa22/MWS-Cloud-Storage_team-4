package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.storage.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.exception.storage.RecoverableStorageException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageObjectNotFoundException;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import org.springframework.stereotype.Component;

@Component
public class MinioWrapper {

  public <T> T execute(Callable<T> operation) {
    try {
      return operation.call();
    } catch (Exception e) {
      throw classifyException(e);
    }
  }

  private RuntimeException classifyException(Exception e) {
    if (e instanceof ExecutionException || e instanceof java.util.concurrent.CompletionException) {
      Throwable cause = e.getCause();

      if (cause instanceof ErrorResponseException ex) {
        if ("NoSuchKey".equals(ex.errorResponse().code()) || ex.response().code() == 404) {
          return new StorageObjectNotFoundException("", e);
        }
      }

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
    if (e instanceof IOException
        || e instanceof InternalException
        || e instanceof InsufficientDataException) {
      return true;
    }

    if (e instanceof ErrorResponseException minioEx) {

      int httpStatus = minioEx.response().code();

      return httpStatus >= HttpResponseStatus.INTERNAL_SERVER_ERROR.code()
          || httpStatus == HttpResponseStatus.TOO_MANY_REQUESTS.code();
    }

    return false;
  }
}
