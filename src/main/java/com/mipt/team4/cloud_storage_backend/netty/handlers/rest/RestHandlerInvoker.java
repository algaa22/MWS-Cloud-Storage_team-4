package com.mipt.team4.cloud_storage_backend.netty.handlers.rest;

import com.mipt.team4.cloud_storage_backend.exception.netty.HandlerMethodInvokeException;
import com.mipt.team4.cloud_storage_backend.exception.netty.HandlerNotFoundException;
import com.mipt.team4.cloud_storage_backend.netty.mapping.RoutedMessage;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpContent;
import jakarta.annotation.PostConstruct;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
  private final Map<String, HandlerMethod> dtoHandlers = new ConcurrentHashMap<>();
  private final Map<String, HandlerMethod> contentHandlers = new ConcurrentHashMap<>();
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
        if (!isValidMethod(method)) {
          continue;
        }
        Class<?> msgClass = method.getParameterTypes()[1];

        RequestMapping annotation = msgClass.getAnnotation(RequestMapping.class);
        if (annotation == null) {
          annotation = method.getAnnotation(RequestMapping.class);
        }

        if (annotation != null) {
          String key = getHandlersKey(annotation.method(), annotation.path());
          HandlerMethod handlerMethod = new HandlerMethod(bean, method);

          if (HttpContent.class.isAssignableFrom(msgClass)) {
            contentHandlers.put(key, handlerMethod);
          } else {
            dtoHandlers.put(key, handlerMethod);
          }
        }
      }
    }
  }

  public void invoke(ChannelHandlerContext ctx, RoutedMessage routedMsg) {
    invoke(ctx, routedMsg.dto(), routedMsg.method(), routedMsg.path());
  }

  public void invoke(ChannelHandlerContext ctx, Object msg, String method, String path) {
    String key = getHandlersKey(method, path);
    HandlerMethod target =
        (msg instanceof HttpContent) ? contentHandlers.get(key) : dtoHandlers.get(key);

    if (target == null) {
      throw new HandlerNotFoundException(msg.getClass());
    }

    try {
      target.method.invoke(target.handler, ctx, msg);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new HandlerMethodInvokeException(e);
    }
  }

  private boolean isValidMethod(Method method) {
    return Modifier.isPublic(method.getModifiers())
        && method.getParameterCount() == 2
        && method.getParameterTypes()[0] == ChannelHandlerContext.class;
  }

  private String getHandlersKey(String method, String path) {
    return method.toUpperCase() + path;
  }

  private record HandlerMethod(Object handler, Method method) {}
}
