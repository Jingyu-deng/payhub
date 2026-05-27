package com.payhub.infra.scheduler;

import com.payhub.infra.common.YamlPropertySourceFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

@Configuration
@PropertySource(
    value = "classpath:scheduler-defaults.yml",
    factory = YamlPropertySourceFactory.class)
public class QuartzConfig implements SchedulerFactoryBeanCustomizer {

  private final AutowireCapableBeanFactory beanFactory;

  public QuartzConfig(AutowireCapableBeanFactory beanFactory) {
    this.beanFactory = beanFactory;
  }

  @Override
  public void customize(SchedulerFactoryBean factory) {
    factory.setJobFactory(new AutowiringSpringBeanJobFactory(beanFactory));
  }

  private static class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory {

    private final AutowireCapableBeanFactory beanFactory;

    AutowiringSpringBeanJobFactory(AutowireCapableBeanFactory beanFactory) {
      this.beanFactory = beanFactory;
    }

    @Override
    protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception {
      return beanFactory.createBean(bundle.getJobDetail().getJobClass());
    }
  }
}
