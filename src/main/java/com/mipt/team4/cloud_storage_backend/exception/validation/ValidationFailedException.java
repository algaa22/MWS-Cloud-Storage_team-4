package com.mipt.team4.cloud_storage_backend.exception.validation;

import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import com.mipt.team4.cloud_storage_backend.netty.handlers.validation.ValidationError;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.Set;
import lombok.Getter;

@Getter
public class ValidationFailedException extends BaseStorageException {
  private final Set<ValidationError> errors;

  public ValidationFailedException(ValidationError error) {
    this(Set.of(error));
  }

  public ValidationFailedException(Set<ValidationError> errors) {
    super("Validation failed", HttpResponseStatus.BAD_REQUEST);

    this.errors = errors;
  }
}
