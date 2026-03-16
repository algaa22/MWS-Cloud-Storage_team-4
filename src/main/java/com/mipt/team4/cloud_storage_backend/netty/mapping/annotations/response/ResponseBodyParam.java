package com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Маркер для включения поля в JSON-тело ответа.
 *
 * <p>Позволяет выборочно формировать структуру ответа на основе полей DTO.
 *
 * <p>Если {@link #value()} не указан, в качестве него используется имя поля/параметра в Java
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface ResponseBodyParam {
  /** Имя ключа в JSON-объекте. */
  String value() default "";

  String defaultValue() default "";
}
