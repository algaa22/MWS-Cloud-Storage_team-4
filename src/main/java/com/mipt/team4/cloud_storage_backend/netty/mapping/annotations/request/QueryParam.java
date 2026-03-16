package com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Извлекает значение из параметров строки запроса (URL Query String).
 *
 * <p>Если значение отсутствует и {@link #required()} установлено в true, будет выброшено
 * исключение.
 *
 * <p>Если {@link #value()} не указан, в качестве него используется имя поля/параметра в Java
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface QueryParam {
  /** Имя параметра в URL. */
  String value() default "";

  String defaultValue() default "";

  boolean required() default true;
}
