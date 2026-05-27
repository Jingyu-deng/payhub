package com.payhub.core.controls.base;

import com.payhub.core.infra.AdapterClient;
import com.payhub.core.infra.ControlClient;
import com.payhub.core.infra.DatabaseClient;
import com.payhub.core.infra.EventPublisher;
import com.payhub.core.infra.IdempotencyClient;
import com.payhub.core.infra.SchedulerClient;
import lombok.Getter;
import lombok.Setter;

/**
 * A {@link Control} whose infrastructure dependencies are injected via setters by the {@code
 * ControlClientImpl} factory at {@code getControl()} time.
 *
 * @param <I> input type
 * @param <O> output type
 */
@Getter
@Setter
public abstract class ControlInjector<I, O> implements Control<I, O> {

  protected AdapterClient adapterClient;
  protected DatabaseClient databaseClient;
  protected IdempotencyClient idempotencyClient;
  protected EventPublisher eventPublisher;
  protected SchedulerClient schedulerClient;
  protected ControlClient controlClient;
}
