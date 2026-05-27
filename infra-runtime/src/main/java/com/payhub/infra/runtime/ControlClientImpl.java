package com.payhub.infra.runtime;

import com.payhub.core.controls.base.Control;
import com.payhub.core.controls.base.ControlInjector;
import com.payhub.core.exception.ServiceNotFoundException;
import com.payhub.core.infra.AdapterClient;
import com.payhub.core.infra.ControlClient;
import com.payhub.core.infra.DatabaseClient;
import com.payhub.core.infra.EventPublisher;
import com.payhub.core.infra.IdempotencyClient;
import com.payhub.core.infra.SchedulerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ControlClientImpl implements ControlClient {

  private final AdapterClient adapterClient;
  private final DatabaseClient databaseClient;
  private final IdempotencyClient idempotencyClient;
  private final EventPublisher eventPublisher;
  private final SchedulerClient schedulerClient;
  private final ApplicationContext applicationContext;

  public ControlClientImpl(
      AdapterClient adapterClient,
      DatabaseClient databaseClient,
      IdempotencyClient idempotencyClient,
      EventPublisher eventPublisher,
      SchedulerClient schedulerClient,
      ApplicationContext applicationContext) {
    this.adapterClient = adapterClient;
    this.databaseClient = databaseClient;
    this.idempotencyClient = idempotencyClient;
    this.eventPublisher = eventPublisher;
    this.schedulerClient = schedulerClient;
    this.applicationContext = applicationContext;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Control<?, ?>> T getControl(Class<T> controlType) {
    var beans = applicationContext.getBeansOfType(controlType);
    if (beans.isEmpty()) {
      throw new ServiceNotFoundException(controlType.getName());
    }
    if (beans.size() > 1) {
      log.warn("Multiple beans for " + controlType.getName() + " — using the first one");
    }
    T control = (T) beans.values().iterator().next();

    if (control instanceof ControlInjector<?, ?> injector) {
      injector.setAdapterClient(adapterClient);
      injector.setDatabaseClient(databaseClient);
      injector.setIdempotencyClient(idempotencyClient);
      injector.setEventPublisher(eventPublisher);
      injector.setSchedulerClient(schedulerClient);
      injector.setControlClient(this);
    }
    return control;
  }
}
