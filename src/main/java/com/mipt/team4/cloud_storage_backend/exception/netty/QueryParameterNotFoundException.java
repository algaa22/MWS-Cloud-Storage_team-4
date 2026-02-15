package com.mipt.team4.cloud_storage_backend.exception.netty;

public class QueryParameterNotFoundException extends Exception {

  public QueryParameterNotFoundException(String paramName) {
    super("Missing required parameter: " + paramName);
  }
}
