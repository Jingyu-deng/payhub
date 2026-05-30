package com.payhub.core.controls.base;

import com.payhub.core.enums.PaymentStatus;

/**
 * A {@link ControlInjector} that reacts to a specific {@link PaymentStatus} domain event. Each
 * concrete subclass declares which event type it handles via {@link #getHandledEventType()}.
 *
 * @param <I> input type (typically {@code PaymentEvent})
 */
public abstract class EventControl<I> extends ControlInjector<I, Void> {

  public abstract PaymentStatus getHandledEventType();
}
