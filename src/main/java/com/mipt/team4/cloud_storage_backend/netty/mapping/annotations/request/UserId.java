package com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Инъекция идентификатора пользователя, прошедшего авторизацию.
 *
 * <p>Значение извлекается из контекста сессии (AttributeKey), установленного JwtAuthHandler.
 *
 * @see com.mipt.team4.cloud_storage_backend.netty.handlers.auth.JwtAuthHandler
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface UserId {}
