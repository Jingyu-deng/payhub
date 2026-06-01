package com.payhub.infra.message;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Holds a static reference to the Spring {@link ApplicationContext} for use by Kafka serializers.
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

  private static volatile ApplicationContext context;

  @Override
  public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
    context = applicationContext;
  }

  public static <T> T getBean(Class<T> requiredType) {
    if (context == null) {
      throw new IllegalStateException("ApplicationContext not set");
    }
    return context.getBean(requiredType);
  }
}
