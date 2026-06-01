package com.payhub.core.domain;

import com.payhub.core.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A domain event carrying a {@link Payment} aggregate snapshot. Implements {@link BaseEvent} — the
 * class itself serves as the event type for dispatch purposes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent implements BaseEvent {

  private Payment payment;
  private long timestamp;

  @Override
  public String key() {
    return payment != null ? payment.getId() : null;
  }
}
