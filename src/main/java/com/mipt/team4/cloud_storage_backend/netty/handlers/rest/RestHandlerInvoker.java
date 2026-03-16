package com.mipt.team4.cloud_storage_backend.netty.handlers.rest;

import com.mipt.team4.cloud_storage_backend.exception.netty.HandlerMethodInvokeException;
import com.mipt.team4.cloud_storage_backend.exception.netty.HandlerNotFoundException;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

@Component
@RequiredArgsConstructor
public class RestHandlerInvoker {
  private final Map<Class<?>, HandlerMethod> handlerMethods = new ConcurrentHashMap<>();
  private final ApplicationContext applicationContext;

  @PostConstruct
  public void init() {
    Map<String, Object> controllers = applicationContext.getBeansWithAnnotation(Controller.class);

    for (Object bean : controllers.values()) {
      Class<?> beanClass = AopUtils.getTargetClass(bean);

      if (!beanClass.isAnnotationPresent(Controller.class)) {
        continue;
      }

      for (Method method : beanClass.getDeclaredMethods()) {
        if (method.getParameterCount() == 2
            && method.getParameterTypes()[0] == ChannelHandlerContext.class) {
          Class<?> msgClass = method.getParameterTypes()[1];
          handlerMethods.put(msgClass, new HandlerMethod(bean, method));
        }
      }
    }
  }

  public void invoke(ChannelHandlerContext ctx, Object msg) {
    HandlerMethod target = handlerMethods.get(msg.getClass());

    if (target == null) {
      throw new HandlerNotFoundException(msg.getClass());
    }

    try {
      target.method.invoke(target.handler, ctx, msg);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new HandlerMethodInvokeException(e);
    }
  }

  private record HandlerMethod(Object handler, Method method) {}
}
