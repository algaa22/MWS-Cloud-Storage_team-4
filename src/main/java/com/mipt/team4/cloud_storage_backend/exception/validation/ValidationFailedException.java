package com.mipt.team4.cloud_storage_backend.exception.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationError;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;

import java.util.List;

public class ValidationFailedException extends Exception {
  private final List<ValidationError> errors;

  public ValidationFailedException(ValidationResult result) {
    this(result.getErrors());
  }

  public ValidationFailedException(List<ValidationError> errors) {
    super(toJsonString(errors));

    this.errors = errors;
  }

  public ValidationFailedException(ValidationError error) {
    super(toJsonString(List.of(error)));

    this.errors = List.of(error);
  }

  public List<ValidationError> getErrors() {
    return errors;
  }

  private static String toJsonString(List<ValidationError> errors) {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = mapper.createObjectNode();
    root.put("error", "Validation failed");

    ArrayNode details = mapper.createArrayNode();
    for (ValidationError error : errors) {
      details.add(error.toJson());
    }

    root.set("details", details);

    return root.toString();
  }
}
