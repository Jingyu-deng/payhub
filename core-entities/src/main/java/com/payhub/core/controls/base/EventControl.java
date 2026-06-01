package com.payhub.core.controls.base;

import com.payhub.core.event.BaseEvent;

/**
 * A {@link ControlInjector} that reacts to a specific {@link BaseEvent} class. Each concrete
 * subclass declares which event class it handles via {@link #getHandledEventType()}.
 *
 * @param <I> input type (the event class, must implement {@link BaseEvent})
 */
public abstract class EventControl<I extends BaseEvent> extends ControlInjector<I, Void> {

  /**
   * Returns the event class this control handles. Dispatch uses {@code Class.isAssignableFrom} so
   * subclasses of the declared type also match.
   */
  public abstract Class<? extends BaseEvent> getHandledEventType();
}
