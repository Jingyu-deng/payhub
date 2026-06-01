package com.payhub.core.event;

/**
 * Marker interface for domain event types.
 *
 * <p>Implementations may override {@link #key()} to provide a Kafka record key for partitioning.
 */
public interface BaseEvent {

  /** Returns the Kafka record key for this event, or {@code null} for no key. */
  default String key() {
    return null;
  }
}
