package com.payhub.core.controls;

import com.payhub.core.infra.AdapterClient;
import com.payhub.core.infra.DatabaseClient;
import com.payhub.core.infra.EventPublisher;
import com.payhub.core.infra.IdempotencyClient;
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

  AdapterClient adapterClient;
  DatabaseClient databaseClient;
  IdempotencyClient idempotencyClient;
  EventPublisher eventPublisher;
}
