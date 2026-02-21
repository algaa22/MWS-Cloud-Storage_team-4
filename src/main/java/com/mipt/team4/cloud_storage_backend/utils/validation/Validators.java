package com.mipt.team4.cloud_storage_backend.utils.validation;

import com.mipt.team4.cloud_storage_backend.exception.validation.ValidationFailedException;
import com.mipt.team4.cloud_storage_backend.model.storage.enums.FileVisibility;
import com.mipt.team4.cloud_storage_backend.service.user.security.JwtService;
import com.mipt.team4.cloud_storage_backend.utils.NumberComparator;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

public class Validators {

  public static ValidationResult any(
      String groupName, String message, ValidationResult... results) {
    for (ValidationResult result : results) {
      if (result.isValid()) {
        return ValidationResult.valid();
      }
    }

    return new ValidationResult(false, List.of(new ValidationError(groupName, message)));
  }

  public static ValidationResult all(ValidationResult... results) {
    ValidationResult combined = ValidationResult.valid();

    for (ValidationResult result : results) {
      combined = combined.combine(result);
    }

    return combined;
  }

  public static ValidationResult validToken(JwtService jwtService, String token) {
    return validate(
        jwtService.isTokenValid(token),
        "User token",
        "User token expired or not valid",
        "VALID_TOKEN");
  }

  public static ValidationResult notNull(String field, Object value) {
    return validate(value != null, field, field + " cannot be null", "NOT_NULL");
  }

  public static ValidationResult validFileName(String field, String fileName) {
    ValidationResult notBlankResult = notBlank(field, fileName);
    if (!notBlankResult.isValid()) {
      return notBlankResult;
    }

    String invalidChars = "/\\:*?\"<>|";

    boolean containsInvalidChar = false;
    for (char c : invalidChars.toCharArray()) {
      if (fileName.indexOf(c) >= 0) {
        containsInvalidChar = true;
        break;
      }
    }

    return validate(
        !containsInvalidChar,
        field,
        field + " contains invalid characters (/, \\, :, *, ?, \", <, >, |)",
        "INVALID_FILENAME");
  }

  public static ValidationResult notBlank(String field, String value) {
    return notBlank(value != null && !value.trim().isBlank(), field);
  }

  public static <T> ValidationResult notBlank(String field, List<T> list) {
    return notBlank(list != null && !list.isEmpty(), field);
  }

  private static ValidationResult notBlank(boolean condition, String field) {
    return validate(condition, field, field + " cannot be blank", "NOT_BLANK");
  }

  public static ValidationResult lengthRange(
      String field, String value, int minLength, int maxLength) {
    if (NumberComparator.greaterThan(minLength, maxLength)) {
      throw new IllegalArgumentException(
          "maxLength (%s) cannot be less than minLength (%s)".formatted(maxLength, minLength));
    }

    return validate(
        value != null && value.length() >= minLength && value.length() <= maxLength,
        "%s must contain %s to %s characters".formatted(field, minLength, maxLength),
        "MAX_LENGTH_" + maxLength);
  }

  public static ValidationResult maxLength(String field, String value, int maxLength) {
    return validate(
        value != null && value.length() <= maxLength,
        "%s cannot exceed %s characters".formatted(field, maxLength),
        "MAX_LENGTH_" + maxLength);
  }

  public static ValidationResult minLength(String field, String value, int minLength) {
    return validate(
        value != null && value.length() >= minLength,
        "%s must contain at least %s characters".formatted(field, minLength),
        "MIN_LENGTH_" + minLength);
  }

  public static ValidationResult isEmail(String email) {
    return pattern("Email", email, "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
  }

  public static ValidationResult pattern(String field, String value, String regex) {
    return validate(
        value != null && value.matches(regex),
        field,
        field + " has invalid format",
        "PATTERN_" + regex);
  }

  public static ValidationResult numberRange(
      String field, Number value, Number minValue, Number maxValue) {
    if (NumberComparator.greaterThan(minValue, maxValue)) {
      throw new IllegalArgumentException(
          String.format("maxValue (%s) cannot be less than minValue (%s)", maxValue, minValue));
    }

    return validate(
        NumberComparator.greaterThanOrEqualsTo(value, minValue)
            && NumberComparator.lessThanOrEqualsTo(value, maxValue),
        field,
        "%s must be between %s and %s".formatted(field, minValue, maxValue),
        "NUMBER_RANGE_%s-%s".formatted(minValue, maxValue));
  }

  public static ValidationResult numberMax(String field, Number value, Number maxValue) {
    return validate(
        NumberComparator.lessThanOrEqualsTo(value, maxValue),
        field,
        "%s must be less than or equal to %s".formatted(field, maxValue),
        "NUMBER_MAX_" + maxValue);
  }

  public static ValidationResult numberMin(String field, Number value, Number minValue) {
    return validate(
        NumberComparator.greaterThanOrEqualsTo(value, minValue),
        field,
        "%s must be greater than or equal to %s".formatted(field, minValue),
        "NUMBER_MIN_" + minValue);
  }

  public static ValidationResult cannotBeNegative(String field, Number value) {
    return validate(
        NumberComparator.greaterThanOrEqualsTo(value, 0),
        field,
        field + " cannot be negative",
        "CANNOT_BE_NEGATIVE");
  }

  public static ValidationResult mustBePositive(String field, Number value) {
    return validate(
        NumberComparator.greaterThan(value, 0),
        field,
        field + " must be positive",
        "MUST_BE_POSITIVE");
  }

  public static ValidationResult isUuid(String field, String uuidStr) {
    try {
      UUID.fromString(uuidStr);
    } catch (IllegalArgumentException e) {
      return ValidationResult.error(field, field + " is not UUID", "IS_UUID");
    }

    return ValidationResult.valid();
  }

  public static void throwExceptionIfNotValid(ValidationResult result)
      throws ValidationFailedException {
    if (!result.isValid()) {
      throw new ValidationFailedException(result);
    }
  }

  public static ValidationResult validateVisibility(String visibility) {
    return validate(
        visibility != null
            && (visibility.equals("public")
                || visibility.equals("private")
                || visibility.equals("link_only")),
        "Visibility",
        "Visibility must be one of this values: {" + FileVisibility.NAMES + "}",
        "VISIBILITY");
  }

  public static ValidationResult notEqualIgnoreCase(
      String field, String firstString, String secondString) {
    return validate(
        !firstString.equalsIgnoreCase(secondString),
        field,
        field + " must not be equal",
        "STRING_IC_NOT_EQUAL");
  }

  public static ValidationResult validate(BooleanSupplier supplier, String field, String message) {
    return validate(supplier.getAsBoolean(), field, message, null);
  }

  public static ValidationResult validate(boolean condition, String field, String message) {
    return validate(condition, field, message, null);
  }

  public static ValidationResult validate(
      boolean condition, String field, String message, String code) {
    if (!condition) {
      return ValidationResult.error(field, message, code);
    }

    return ValidationResult.valid();
  }
}
