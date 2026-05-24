package com.payhub.infra.runtime;

import com.payhub.core.adapters.Adapter;
import java.util.ServiceLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AdapterBeanDefinitionRegistrar implements BeanDefinitionRegistryPostProcessor {

  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    ServiceLoader.load(Adapter.class).stream()
        .map(ServiceLoader.Provider::type)
        .forEach(
            implClass -> {
              BeanDefinition def =
                  BeanDefinitionBuilder.genericBeanDefinition(implClass)
                      .setScope(BeanDefinition.SCOPE_SINGLETON)
                      .getBeanDefinition();
              registry.registerBeanDefinition(implClass.getSimpleName(), def);
              log.info("Registered adapter bean definition: " + implClass.getName());
            });
  }

  @Override
  public void postProcessBeanFactory(
      org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) {
    // no-op
  }
}
