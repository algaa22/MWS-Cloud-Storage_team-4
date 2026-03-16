package com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Устанавливает HTTP код ответа.
 *
 * <p>Может быть установлена как над классом ответа, так и в качестве параметра.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.PARAMETER, ElementType.FIELD})
public @interface ResponseStatus {
  /** HTTP код (например, 201 для Created). По умолчанию 200. */
  int value() default 200;

  int defaultValue() default 200;
}
