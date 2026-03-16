package com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.response;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ResponseStatus {
  int value() default 200;
}
