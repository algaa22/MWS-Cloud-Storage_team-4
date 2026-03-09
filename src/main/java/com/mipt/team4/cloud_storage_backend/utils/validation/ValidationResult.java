package com.mipt.team4.cloud_storage_backend.utils.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public class ValidationResult {

  private final boolean valid;
  private final List<ValidationError> errors;

  public ValidationResult(boolean valid, List<ValidationError> errors) {
    this.valid = valid;
    this.errors = Collections.unmodifiableList(errors);
  }

  public static ValidationResult valid() {
    return new ValidationResult(true, Collections.emptyList());
  }

  public static ValidationResult error(String field, String message) {
    return error(field, message, null);
  }

  public static ValidationResult error(String field, String message, String code) {
    ValidationError error = new ValidationError(field, message, code);
    return new ValidationResult(false, List.of(error));
  }

  public static ValidationResult errors(List<ValidationError> errors) {
    return new ValidationResult(false, errors);
  }

  public ValidationResult thenCombine(Supplier<ValidationResult> otherSupplier) {
    if (this.valid) {
      return this.combine(otherSupplier.get());
    }

    return this;
  }

  public ValidationResult combine(ValidationResult other) {
    if (this.valid && other.valid) {
      return valid();
    }

    List<ValidationError> mergedErrors = new ArrayList<>(this.errors);
    mergedErrors.addAll(other.errors);

    return errors(mergedErrors);
  }

  public boolean isValid() {
    return valid;
  }

  public List<ValidationError> getErrors() {
    return errors;
  }
}
