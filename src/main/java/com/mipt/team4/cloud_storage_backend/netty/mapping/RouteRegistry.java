package com.mipt.team4.cloud_storage_backend.netty.mapping;

import com.mipt.team4.cloud_storage_backend.exception.netty.mapping.ScanRouteException;
import com.mipt.team4.cloud_storage_backend.netty.mapping.annotations.request.RequestMapping;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

@Component
public class RouteRegistry {
  private static final String MODEL_PACKAGE = "com.mipt.team4.cloud_storage_backend.model";
  private final Map<String, Class<?>> routes = new ConcurrentHashMap<>();

  @PostConstruct
  public void init() {
    scanAndRegisterRoutes();
  }

  public Class<?> getDto(String method, String path) {
    return routes.get(mapToRouteKey(method, path));
  }

  public boolean isRegisteredDto(Class<?> clazz) {
    return routes.containsValue(clazz);
  }

  private void scanAndRegisterRoutes() {
    ClassPathScanningCandidateComponentProvider scanner =
        new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter(new AnnotationTypeFilter(RequestMapping.class));

    Set<BeanDefinition> candidates = scanner.findCandidateComponents(MODEL_PACKAGE);

    for (BeanDefinition beanDefinition : candidates) {
      try {
        Class<?> dtoClass = Class.forName(beanDefinition.getBeanClassName());
        RequestMapping mapping = dtoClass.getAnnotation(RequestMapping.class);

        String routeKey = mapToRouteKey(mapping.method(), mapping.path());
        routes.put(routeKey, dtoClass);
      } catch (ClassNotFoundException e) {
        throw new ScanRouteException(beanDefinition.getBeanClassName(), e);
      }
    }
  }

  private String mapToRouteKey(String method, String path) {
    return method.toUpperCase() + path;
  }
}
