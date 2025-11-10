package com.mipt.team4.cloud_storage_backend.utils.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public record ValidationError(String field, String message, String code) {
  public ValidationError(String field, String message) {
    this(field, message, null);
  }

  public JsonNode toJson() {
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode node = mapper.createObjectNode();

    node.put("field", field);
    node.put("message", message);

    if (code != null) {
      node.put("code", code);
    }

    return node;
  }
}
