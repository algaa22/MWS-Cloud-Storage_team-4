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
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

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

  public static MappedParameter from(Parameter parameter) {
    return Stream.<Function<Parameter, Optional<MappedParameter>>>of(
            MappedParameter::tryQueryParam,
            MappedParameter::tryRequestHeader,
            MappedParameter::tryRequestBodyParam,
            MappedParameter::tryResponseBody,
            MappedParameter::tryResponseHeader,
            MappedParameter::tryResponseBodyParam,
            MappedParameter::tryResponseStatus,
            MappedParameter::tryUserId)
        .map(func -> func.apply(parameter))
        .flatMap(Optional::stream)
        .findFirst()
        .orElse(null);
  }

  private static Optional<MappedParameter> tryQueryParam(Parameter p) {
    return Optional.ofNullable(p.getAnnotation(QueryParam.class))
        .map(a -> create(p, a.value(), SourceType.QUERY, a.defaultValue(), a.required()));
  }

  private static Optional<MappedParameter> tryRequestHeader(Parameter parameter) {
    return Optional.ofNullable(parameter.getAnnotation(RequestHeader.class))
        .map(
            annotation ->
                create(
                    parameter,
                    annotation.value().isEmpty()
                        ? toHttpHeaderName(parameter.getName())
                        : annotation.value(),
                    SourceType.HEADER,
                    annotation.defaultValue(),
                    annotation.required()));
  }

  private static Optional<MappedParameter> tryRequestBodyParam(Parameter parameter) {
    return Optional.ofNullable(parameter.getAnnotation(RequestBodyParam.class))
        .map(
            annotation ->
                create(
                    parameter,
                    annotation.value(),
                    SourceType.BODY_PARAM,
                    annotation.defaultValue(),
                    annotation.required()));
  }

  private static Optional<MappedParameter> tryResponseBodyParam(Parameter parameter) {
    return Optional.ofNullable(parameter.getAnnotation(ResponseBodyParam.class))
        .map(
            annotation ->
                create(
                    parameter,
                    annotation.value(),
                    SourceType.BODY_PARAM,
                    annotation.defaultValue(),
                    true));
  }

  private static Optional<MappedParameter> tryResponseBody(Parameter parameter) {
    return Optional.ofNullable(parameter.getAnnotation(RequestBody.class))
        .map(annotation -> create(parameter, null, SourceType.BODY, null, annotation.required()));
  }

  private static Optional<MappedParameter> tryUserId(Parameter parameter) {
    return Optional.ofNullable(parameter.getAnnotation(UserId.class))
        .map(a -> create(parameter, null, SourceType.AUTH, null, true));
  }

  private static Optional<MappedParameter> tryResponseHeader(Parameter parameter) {
    return Optional.ofNullable(parameter.getAnnotation(ResponseHeader.class))
        .map(
            annotation ->
                create(
                    parameter,
                    annotation.value().isEmpty()
                        ? toHttpHeaderName(parameter.getName())
                        : annotation.value(),
                    SourceType.HEADER,
                    annotation.defaultValue(),
                    false));
  }

  private static Optional<MappedParameter> tryResponseStatus(Parameter parameter) {
    return Optional.ofNullable(parameter.getAnnotation(ResponseStatus.class))
        .map(annotation -> create(parameter, null, SourceType.STATUS, null, false));
  }

  private static MappedParameter create(
      Parameter parameter,
      String mappedName,
      SourceType source,
      String defaultValue,
      boolean required) {
    return new MappedParameter(
        parameter.getName(),
        (mappedName == null || mappedName.isBlank()) ? parameter.getName() : mappedName,
        parameter.getType(),
        source,
        defaultValue,
        required);
  }

  /**
   * Превращает camelCase в X-Kebab-Case.
   *
   * <p>Пример: fileSize -> X-File-Size
   */
  private static String toHttpHeaderName(String fieldName) {
    StringBuilder result = new StringBuilder("X-");

    for (int i = 0; i < fieldName.length(); i++) {
      char c = fieldName.charAt(i);

      if (Character.isUpperCase(c)) {
        result.append("-").append(c);
      } else if (i == 0) {
        result.append(Character.toUpperCase(c));
      } else {
        result.append(c);
      }
    }

    return result.toString();
  }
}
