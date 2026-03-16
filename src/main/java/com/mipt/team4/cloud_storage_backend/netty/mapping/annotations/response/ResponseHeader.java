package com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Отправляет значение поля в заголовке HTTP-ответа.
 *
 * <p>Аналогично {@code RequestHeader}, поддерживает авто-генерацию имен (X-Kebab-Case).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface ResponseHeader {
  String value() default "";

  String defaultValue() default "";
}
