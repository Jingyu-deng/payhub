package com.payhub.core.template;

import com.payhub.core.controls.NotifyPartnerTemplate;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.event.BaseEvent;
import com.payhub.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NotifyPartnerControl extends NotifyPartnerTemplate {

  @Override
  public Class<? extends BaseEvent> getHandledEventType() {
    return PaymentEvent.class;
  }

  @Override
  protected String buildNotificationBody(PaymentEvent event) {
    return JsonUtils.toJson(event);
  }
}
