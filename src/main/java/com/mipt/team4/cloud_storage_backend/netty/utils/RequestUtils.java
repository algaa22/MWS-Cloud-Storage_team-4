package com.mipt.team4.cloud_storage_backend.netty.utils;

import com.mipt.team4.cloud_storage_backend.exception.netty.HeaderNotFoundException;
import com.mipt.team4.cloud_storage_backend.exception.netty.QueryParameterNotFoundException;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RequestUtils {

  public static String getRequiredQueryParam(HttpRequest request, String paramName)
      throws QueryParameterNotFoundException {
    return getQueryParam(request, paramName)
        .orElseThrow(() -> new QueryParameterNotFoundException(paramName));
  }

  // В классе RequestUtils
  public static UUID getRequiredUuidQueryParam(HttpRequest request, String paramName)
      throws QueryParameterNotFoundException {
    String value = getRequiredQueryParam(request, paramName);
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      throw new QueryParameterNotFoundException(paramName + " must be a valid UUID");
    }
  }

  public static UUID getOptionalUuidQueryParam(HttpRequest request, String paramName) {
    String value = getOptionalQueryParam(request, paramName);
    if (value == null || value.isEmpty()) {
      return null;
    }
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public static String getOptionalQueryParam(HttpRequest request, String paramName) {
    return getQueryParam(request, paramName).orElse(null);
  }

  public static String getQueryParam(HttpRequest request, String paramName, String defaultValue) {
    return getQueryParam(request, paramName).orElse(defaultValue);
  }

  public static Optional<String> getQueryParam(HttpRequest request, String paramName) {
    QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());
    Map<String, List<String>> parameters = queryDecoder.parameters();

    if (parameters.containsKey(paramName) && !parameters.get(paramName).isEmpty()) {
      String value = parameters.get(paramName).getFirst();

      return Optional.of(value);
    }

    return Optional.empty();
  }

  public static String getRequiredHeader(HttpRequest request, String headerName)
      throws HeaderNotFoundException {
    return getHeader(request, headerName)
        .orElseThrow(() -> new HeaderNotFoundException(headerName));
  }

  public static String getHeader(HttpRequest request, String headerName, String defaultValue) {
    return getHeader(request, headerName).orElse(defaultValue);
  }

  public static Optional<String> getHeader(HttpRequest request, String headerName) {
    return Optional.ofNullable(request.headers().get(headerName));
  }
}
