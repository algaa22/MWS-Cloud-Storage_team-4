package com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Указывает маршрут и HTTP-метод для обработки запроса.
 *
 * <p>Применяется к классам DTO или методам хендлеров для связки с конкретным эндпоинтом.
 *
 * @see com.mipt.team4.cloud_storage_backend.netty.handlers.rest.RestHandlerInvoker
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequestMapping {
  /** HTTP метод (GET, POST, PATCH, DELETE и т.д.) */
  String method();

  /** Путь ресурса, например "/api/files/download" */
  String path();
}
