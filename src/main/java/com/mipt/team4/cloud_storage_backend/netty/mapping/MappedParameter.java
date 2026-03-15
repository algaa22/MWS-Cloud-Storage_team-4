package com.mipt.team4.cloud_storage_backend.netty.mapping;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestBodyParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.ResponseBody;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.ResponseHeader;
import java.lang.reflect.Parameter;

public record MappedParameter(
    String name, Class<?> type, SourceType source, String defaultValue, boolean required) {
  public enum SourceType {
    QUERY,
    HEADER,
    BODY
  }

  public static MappedParameter from(Parameter parameter) {
    if (parameter.isAnnotationPresent(QueryParam.class)) {
      QueryParam annotation = parameter.getAnnotation(QueryParam.class);
      return new MappedParameter(
          annotation.value(),
          parameter.getType(),
          SourceType.QUERY,
          annotation.defaultValue(),
          annotation.required());
    } else if (parameter.isAnnotationPresent(RequestHeader.class)) {
      RequestHeader annotation = parameter.getAnnotation(RequestHeader.class);
      return new MappedParameter(
          annotation.value(),
          parameter.getType(),
          SourceType.HEADER,
          annotation.defaultValue(),
          annotation.required());
    } else if (parameter.isAnnotationPresent(RequestBodyParam.class)) {
      RequestBodyParam annotation = parameter.getAnnotation(RequestBodyParam.class);
      return new MappedParameter(
          annotation.value(),
          parameter.getType(),
          SourceType.BODY,
          annotation.defaultValue(),
          annotation.required());
    } else if (parameter.isAnnotationPresent(ResponseHeader.class)) {
      ResponseHeader annotation = parameter.getAnnotation(ResponseHeader.class);
      return new MappedParameter(
          annotation.value(),
          parameter.getType(),
          SourceType.HEADER,
          annotation.defaultValue(),
          false);
    } else if (parameter.isAnnotationPresent(ResponseBody.class)) {
      ResponseBody annotation = parameter.getAnnotation(ResponseBody.class);
      return new MappedParameter(
          annotation.value(),
          parameter.getType(),
          SourceType.BODY,
          annotation.defaultValue(),
          false);
    }

    return null;
  }
}
