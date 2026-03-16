package com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Извлекает значение из заголовков HTTP-запроса.
 *
 * <p>Поддерживает автоматическую конвертацию: если {@link #value()} не указан, имя параметра {@code
 * fileSize} будет преобразовано в заголовок {@code X-File-Size}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface RequestHeader {
  /** Явное имя заголовка. Если пусто, генерируется автоматически из имени поля. */
  String value() default "";

  String defaultValue() default "";

  boolean required() default true;
}
