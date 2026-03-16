package com.mipt.team4.cloud_storage_backend.exception.netty.mapping;

import com.mipt.team4.cloud_storage_backend.exception.FatalStorageException;
import com.mipt.team4.cloud_storage_backend.netty.mapping.MappedParameter.SourceType;

public class UnknownRequestSourceTypeException extends FatalStorageException {
  public UnknownRequestSourceTypeException(String paramName, SourceType source) {
    super(
        "Unknown request parameter source type: param=%s, source=%s".formatted(paramName, source));
  }
}
