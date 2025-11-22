package com.mipt.team4.cloud_storage_backend.exception.validation;

import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationError;

public class ParseException extends ValidationFailedException {
  public <T> ParseException(String field, Class<?> expectedType, String value) {
    super(
        new ValidationError(
            field,
            "Field " + field + " with value " + value + " must be " + expectedType,
            "PARSE_" + expectedType));
  }
}
