package com.payhub.infra.runtime;

import com.payhub.core.controls.base.Control;
import com.payhub.core.controls.base.ControlInjector;
import com.payhub.core.controls.base.EventControl;
import com.payhub.core.event.BaseEvent;
import com.payhub.core.exception.ServiceNotFoundException;
import com.payhub.core.infra.*;
import java.util.List;
import java.util.Map;
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
  private final HttpClient httpClient;
  private final ApplicationContext applicationContext;

  public ControlClientImpl(
      AdapterClient adapterClient,
      DatabaseClient databaseClient,
      IdempotencyClient idempotencyClient,
      EventPublisher eventPublisher,
      SchedulerClient schedulerClient,
      HttpClient httpClient,
      ApplicationContext applicationContext) {
    this.adapterClient = adapterClient;
    this.databaseClient = databaseClient;
    this.idempotencyClient = idempotencyClient;
    this.eventPublisher = eventPublisher;
    this.schedulerClient = schedulerClient;
    this.httpClient = httpClient;
    this.applicationContext = applicationContext;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Control<?, ?>> T getControl(Class<T> controlType) {
    Map<String, T> beans = applicationContext.getBeansOfType(controlType);
    if (beans.isEmpty()) {
      throw new ServiceNotFoundException(controlType.getName());
    }
    if (beans.size() > 1) {
      log.warn("Multiple beans for " + controlType.getName() + " — using the first one");
    }
    T control = beans.values().iterator().next();

    if (control instanceof ControlInjector<?, ?> injector) {
      injector.setAdapterClient(adapterClient);
      injector.setDatabaseClient(databaseClient);
      injector.setIdempotencyClient(idempotencyClient);
      injector.setEventPublisher(eventPublisher);
      injector.setSchedulerClient(schedulerClient);
      injector.setControlClient(this);
      injector.setHttpClient(httpClient);
    }
    return control;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<EventControl<BaseEvent>> getEventControls() {
    Map<String, EventControl> allControls = applicationContext.getBeansOfType(EventControl.class);
    return allControls.values().stream()
        .peek(
            ec -> {
              if (ec instanceof ControlInjector<?, ?> injector) {
                injector.setAdapterClient(adapterClient);
                injector.setDatabaseClient(databaseClient);
                injector.setIdempotencyClient(idempotencyClient);
                injector.setEventPublisher(eventPublisher);
                injector.setSchedulerClient(schedulerClient);
                injector.setControlClient(this);
                injector.setHttpClient(httpClient);
              }
            })
        .map(ec -> (EventControl<BaseEvent>) ec)
        .toList();
  }
}
