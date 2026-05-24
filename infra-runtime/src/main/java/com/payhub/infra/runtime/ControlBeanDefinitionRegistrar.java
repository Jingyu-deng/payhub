package com.payhub.infra.runtime;

import com.payhub.core.controls.Control;
import java.util.ServiceLoader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ControlBeanDefinitionRegistrar implements BeanDefinitionRegistryPostProcessor {

  @Override
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    ServiceLoader.load(Control.class).stream()
        .map(ServiceLoader.Provider::type)
        .forEach(
            implClass -> {
              BeanDefinition def =
                  BeanDefinitionBuilder.genericBeanDefinition(implClass)
                      .setScope(BeanDefinition.SCOPE_PROTOTYPE)
                      .getBeanDefinition();
              registry.registerBeanDefinition(implClass.getSimpleName(), def);
              log.info("Registered control bean definition: " + implClass.getName());
            });
  }

  @Override
  public void postProcessBeanFactory(
      org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory) {
    // no-op
  }
}
