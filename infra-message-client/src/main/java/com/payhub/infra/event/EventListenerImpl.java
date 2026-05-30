package com.payhub.infra.event;

import com.payhub.core.controls.base.EventControl;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.infra.ControlClient;
import com.payhub.core.infra.EncryptionClient;
import com.payhub.core.infra.EventListener;
import com.payhub.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventListenerImpl implements EventListener {

  private final EncryptionClient encryptionClient;
  private final ControlClient controlClient;

  public EventListenerImpl(EncryptionClient encryptionClient, ControlClient controlClient) {
    this.encryptionClient = encryptionClient;
    this.controlClient = controlClient;
  }

  @KafkaListener(topics = "payment-events")
  public void onMessage(String encryptedPayload) {
    String json = encryptionClient.decrypt(encryptedPayload);
    PaymentEvent event = JsonUtils.fromJson(json, PaymentEvent.class);
    log.info("Event received: type={}, paymentId={}", event.getType(), event.getPayment().getId());

    for (EventControl<PaymentEvent> control : controlClient.getEventControls(event.getType())) {
      control.execute(event);
    }
  }

  @Override
  public void onEvent(PaymentEvent event) {
    onMessage(JsonUtils.toJson(event));
  }
}
