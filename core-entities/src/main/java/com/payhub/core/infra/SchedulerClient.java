package com.payhub.core.infra;

import com.payhub.core.controls.base.Control;
import java.time.Duration;

/**
 * Port for triggering a {@link Control} on a schedule — recurring or one-shot. Implementations
 * handle the mechanics (in-memory executor, Kafka, Quartz, etc.) so callers only declare what to
 * run and when.
 */
public interface SchedulerClient {

  /**
   * Invokes {@code controlType.execute(request)} repeatedly at the given {@code interval} until
   * {@code maxDuration} elapses.
   *
   * @return the job key that can be used to cancel the schedule via {@link #cancel(String)}
   */
  <I> String scheduleRecurring(
      Class<? extends Control<I, ?>> controlType,
      I request,
      Duration interval,
      Duration maxDuration);

  /**
   * Invokes {@code controlType.execute(request)} once after the given {@code delay}.
   *
   * @return the job key that can be used to cancel the schedule via {@link #cancel(String)}
   */
  <I> String scheduleOnce(Class<? extends Control<I, ?>> controlType, I request, Duration delay);

  /** Cancels the scheduled job identified by the given {@code jobKey}. */
  void cancel(String jobKey);
}
