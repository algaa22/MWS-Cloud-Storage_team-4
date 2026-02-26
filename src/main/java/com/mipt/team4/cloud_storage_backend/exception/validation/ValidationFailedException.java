package com.mipt.team4.cloud_storage_backend.exception.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mipt.team4.cloud_storage_backend.exception.BaseStorageException;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationError;
import com.mipt.team4.cloud_storage_backend.utils.validation.ValidationResult;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.util.List;
import lombok.Getter;

@Getter
public class ValidationFailedException extends BaseStorageException {
  private final List<ValidationError> errors;

  public ValidationFailedException(ValidationResult result) {
    this(result.getErrors());
  }

  public ValidationFailedException(List<ValidationError> errors) {
    super(toJsonString(errors), HttpResponseStatus.BAD_REQUEST);

    this.errors = errors;
  }

  public ValidationFailedException(ValidationError error) {
    super(toJsonString(List.of(error)), HttpResponseStatus.BAD_REQUEST);

    this.errors = List.of(error);
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
