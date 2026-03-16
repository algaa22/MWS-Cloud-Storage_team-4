package com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Извлекает конкретное поле из JSON-тела HTTP-запроса.
 *
 * <p>Используется в тех случаях, когда тело запроса представляет собой JSON-объект, и необходимо
 * привязать значение по ключу к конкретному параметру конструктора DTO.
 *
 * <p>Если {@link #value()} не указан, в качестве него используется имя поля/параметра в Java
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface RequestBodyParam {
  /** Имя ключа в JSON-объекте. */
  String value() default "";

  String defaultValue() default "";

  boolean required() default true;
}
