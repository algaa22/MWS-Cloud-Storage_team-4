package com.mipt.team4.cloud_storage_backend.netty.mapping;

import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.QueryParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestBody;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestBodyParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.UserId;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseBodyParam;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseHeader;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response.ResponseStatus;
import java.lang.reflect.Parameter;

public record MappedParameter(
    String name,
    String mappedName,
    Class<?> type,
    SourceType source,
    String defaultValue,
    boolean required) {
  public enum SourceType {
    QUERY,
    HEADER,
    BODY_PARAM,
    BODY,
    AUTH,
    STATUS
  }

  public static MappedParameter from(Parameter parameter) { // TODO: duplicate
    if (parameter.isAnnotationPresent(QueryParam.class)) {
      QueryParam annotation = parameter.getAnnotation(QueryParam.class);
      return new MappedParameter(
          parameter.getName(),
          !annotation.value().isBlank() ? annotation.value() : parameter.getName(),
          parameter.getType(),
          SourceType.QUERY,
          annotation.defaultValue(),
          annotation.required());
    } else if (parameter.isAnnotationPresent(RequestHeader.class)) {
      RequestHeader annotation = parameter.getAnnotation(RequestHeader.class);
      return new MappedParameter(
          parameter.getName(),
          annotation.value(),
          parameter.getType(),
          SourceType.HEADER,
          annotation.defaultValue(),
          annotation.required());
    } else if (parameter.isAnnotationPresent(RequestBodyParam.class)) {
      RequestBodyParam annotation = parameter.getAnnotation(RequestBodyParam.class);
      return new MappedParameter(
          parameter.getName(),
          !annotation.value().isBlank() ? annotation.value() : parameter.getName(),
          parameter.getType(),
          SourceType.BODY_PARAM,
          annotation.defaultValue(),
          annotation.required());
    } else if (parameter.isAnnotationPresent(RequestBody.class)) {
      RequestBody annotation = parameter.getAnnotation(RequestBody.class);
      return new MappedParameter(
          parameter.getName(),
          !annotation.value().isBlank() ? annotation.value() : parameter.getName(),
          parameter.getType(),
          SourceType.BODY,
          null,
          annotation.required());
    } else if (parameter.isAnnotationPresent(ResponseHeader.class)) {
      ResponseHeader annotation = parameter.getAnnotation(ResponseHeader.class);
      return new MappedParameter(
          parameter.getName(),
          annotation.value(), // TODO: автомат. преобразование имени параметра в name
          parameter.getType(),
          SourceType.HEADER,
          annotation.defaultValue(),
          false);
    } else if (parameter.isAnnotationPresent(ResponseBodyParam.class)) {
      ResponseBodyParam annotation = parameter.getAnnotation(ResponseBodyParam.class);
      return new MappedParameter(
          parameter.getName(),
          !annotation.value().isBlank() ? annotation.value() : parameter.getName(),
          parameter.getType(),
          SourceType.BODY_PARAM,
          annotation.defaultValue(),
          false);
    } else if (parameter.isAnnotationPresent(UserId.class)) {
      return new MappedParameter(
          parameter.getName(), null, parameter.getType(), SourceType.AUTH, null, false);
    } else if (parameter.isAnnotationPresent(ResponseStatus.class)) {
      ResponseStatus annotation = parameter.getAnnotation(ResponseStatus.class);
      return new MappedParameter(
          parameter.getName(),
          String.valueOf(annotation.value()),
          parameter.getType(),
          SourceType.STATUS,
          null,
          false);
    }

    return null;
  }
}
