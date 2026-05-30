package com.payhub.core.template;

import com.payhub.core.controls.base.EventControl;
import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.infra.HttpClient;
import com.payhub.core.utils.JsonUtils;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotifyPartnerControl extends EventControl<PaymentEvent> {

  @Override
  public PaymentStatus getHandledEventType() {
    return PaymentStatus.COMPLETED;
  }

  @Override
  public Void execute(PaymentEvent event) {
    Payment payment = event.getPayment();
    String notifyUrl = payment.getNotifyUrl();
    if (notifyUrl == null || notifyUrl.isBlank()) {
      return null;
    }
    String body = JsonUtils.toJson(event);
    HttpClient.Response response =
        httpClient.post(notifyUrl, Map.of("Content-Type", "application/json"), body);
    log.info("Partner notified: url={}, status={}", notifyUrl, response.getStatusCode());
    return null;
  }
}
