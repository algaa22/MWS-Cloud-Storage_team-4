package com.mipt.team4.cloud_storage_backend.netty.utils;

import io.netty.handler.codec.http.HttpRequest;
import java.util.Optional;

public class RequestUtils {

  public static String getHeader(HttpRequest request, String headerName, String defaultValue) {
    return getHeader(request, headerName).orElse(defaultValue);
  }

  public static Optional<String> getHeader(HttpRequest request, String headerName) {
    return Optional.ofNullable(request.headers().get(headerName));
  }
}
