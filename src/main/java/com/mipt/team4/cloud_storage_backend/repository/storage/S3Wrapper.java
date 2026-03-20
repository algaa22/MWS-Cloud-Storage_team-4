package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageObjectNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.apache.hc.core5.http.HttpStatus;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Обёртка над S3 клиентом для централизованной обработки исключений AWS SDK v2.
 *
 * <p>Реализует паттерн "Execute Strategy", инкапсулируя логику анализа сетевых и протокольных
 * ошибок S3-хранилища. Трансформирует специфичные ошибки SDK в иерархию исключений приложения
 * {@link com.mipt.team4.cloud_storage_backend.exception.BaseStorageException}.
 */
@Component
public class S3Wrapper {

  /**
   * Выполняет операцию в контексте обработки ошибок S3.
   *
   * @param operation лямбда или Callable, содержащий вызов S3Client.
   * @param <T> тип возвращаемого значения операции.
   * @return результат выполнения операции.
   * @throws StorageObjectNotFoundException если объект не найден в бакете.
   * @throws RecoverableStorageException при временных сбоях (сеть, 5xx ошибки S3).
   * @throws FatalStorageException при критических ошибках (неверные credentials, 403 Forbidden).
   */
  public <T> T execute(Callable<T> operation) {
    try {
      return operation.call();
    } catch (Exception e) {
      throw classifyException(e);
    }
  }

  /**
   * Анализирует дерево причин исключения и классифицирует его по степени тяжести.
   *
   * <p>Особое внимание уделяется {@link ExecutionException}, так как при асинхронных вызовах SDK
   * реальная ошибка часто скрыта внутри Wrapper-исключения.
   */
  private RuntimeException classifyException(Exception e) {
    if (e instanceof ExecutionException || e instanceof java.util.concurrent.CompletionException) {
      Throwable cause = e.getCause();
      if (cause instanceof Exception) {
        return classifyException((Exception) cause);
      }
    }

    if (e instanceof S3Exception s3Ex) {
      String errorCode = s3Ex.awsErrorDetails().errorCode();

      if (e instanceof NoSuchKeyException
          || e instanceof NoSuchBucketException
          || s3Ex.statusCode() == HttpStatus.SC_NOT_FOUND) {
        return new StorageObjectNotFoundException("S3 object or bucket not found", e);
      }

      if ("NoSuchUpload".equals(errorCode)) {
        return new UploadSessionNotFoundException(new RecoverableStorageException(e));
      }

      if (s3Ex.statusCode() == HttpStatus.SC_FORBIDDEN) {
        return new FatalStorageException("S3 Access Denied (check credentials/permissions)", e);
      }
    }

    if (e instanceof InterruptedException) {
      Thread.currentThread().interrupt();
      return new FatalStorageException("Thread was interrupted during S3 operation", e);
    }

    if (isRecoverable(e)) {
      return new RecoverableStorageException(e);
    }

    return new FatalStorageException("Critical S3 error", e);
  }

  /**
   * Определяет, является ли ошибка временной (сетевой лаг, перегрузка S3).
   *
   * <p>К восстановимым относятся:
   *
   * <ul>
   *   <li>Ошибки ввода-вывода (IOException)
   *   <li>Внутренние ошибки S3 (HTTP 500, 503)
   *   <li>Превышение лимитов запросов (HTTP 429 Too Many Requests)
   * </ul>
   */
  private boolean isRecoverable(Exception e) {
    if (e instanceof IOException || e instanceof SdkClientException) {
      return true;
    }

    if (e instanceof S3Exception s3Ex) {
      int httpStatus = s3Ex.statusCode();
      return httpStatus >= 500 || httpStatus == HttpStatus.SC_TOO_MANY_REQUESTS;
    }

    return false;
  }
}
