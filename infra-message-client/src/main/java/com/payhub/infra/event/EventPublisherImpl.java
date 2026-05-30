package com.payhub.infra.event;

import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.infra.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventPublisherImpl implements EventPublisher {

  @Override
  public void publish(PaymentEvent event) {
    log.info("[IN-MEMORY] Event published: " + event.getType() + " order=" + event.getPayment().getOrderId());
  }
}
