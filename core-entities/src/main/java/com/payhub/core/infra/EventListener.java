package com.payhub.core.infra;

import com.payhub.core.domain.PaymentEvent;

/**
 * Infrastructure interface for receiving domain events from a message broker. Implementations
 * handle deserialization and dispatch to {@code EventControl} instances.
 */
public interface EventListener {

  void onEvent(PaymentEvent event);
}
