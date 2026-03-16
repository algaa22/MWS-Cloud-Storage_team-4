package com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Указывает, что всё тело HTTP-запроса должно быть передано в этот параметр.
 *
 * <p>Используется только для параметров типа {@code byte[]}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface RequestBody {
  boolean required() default true;
}
