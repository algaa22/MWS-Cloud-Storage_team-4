package com.mipt.team4.cloud_storage_backend.repository.storage;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.exception.RecoverableStorageException;
import com.mipt.team4.cloud_storage_backend.exception.storage.StorageObjectNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.transfer.UploadSessionNotFoundException;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.springframework.stereotype.Component;

/**
 * Обертка над клиентом MinIO для централизованной обработки исключений.
 *
 * <p>Реализует паттерн "Execute Strategy", инкапсулируя логику анализа сетевых и протокольных
 * ошибок S3-хранилища. Трансформирует специфичные ошибки SDK в иерархию исключений приложения
 * {@link com.mipt.team4.cloud_storage_backend.exception.BaseStorageException}.
 */
@Component
public class MinioWrapper {

    /**
     * Выполняет операцию в контексте обработки ошибок MinIO.
     *
     * @param operation лямбда или Callable, содержащий вызов MinioClient.
     * @param <T>       тип возвращаемого значения операции.
     * @return результат выполнения операции.
     * @throws StorageObjectNotFoundException если объект не найден в бакете.
     * @throws RecoverableStorageException    при временных сбоях (сеть, 5xx ошибки S3).
     * @throws FatalStorageException          при критических ошибках (неверные credentials, 403 Forbidden).
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

            if (cause instanceof ErrorResponseException ex) {
                String code = ex.errorResponse().code();

                if ("NoSuchKey".equals(code) || ex.response().code() == 404) {
                    return new StorageObjectNotFoundException("", e);
                }

                if ("NoSuchUpload".equals(code)) {
                    return new UploadSessionNotFoundException(new RecoverableStorageException(ex));
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
            return new RecoverableStorageException(e);
        }

        return new FatalStorageException("Critical MinIO error", e);
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
        if (e instanceof IOException
                || e instanceof InternalException
                || e instanceof InsufficientDataException) {
            return true;
        }

        if (e instanceof ErrorResponseException minioEx) {
            int httpStatus = minioEx.response().code();

            return httpStatus >= HttpResponseStatus.INTERNAL_SERVER_ERROR.code() // Все 500-ые статусы
                    || httpStatus == HttpResponseStatus.TOO_MANY_REQUESTS.code();
        }

        return false;
    }
}
