package com.payhub.core.controls;

import com.payhub.core.controls.base.EventControl;
import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.exception.PartnerNotificationException;
import com.payhub.core.http.DynamicHttpApi;
import com.payhub.core.infra.HttpClient;
import lombok.extern.slf4j.Slf4j;

/**
 * Template for notifying a partner via their webhook URL when a payment reaches a terminal state.
 * Extracts the notify URL from the event, builds the notification body, and delivers it via HTTP
 * POST through a declarative {@link DynamicHttpApi} proxy.
 *
 * <p>On retryable failures (5xx, network errors) throws {@link PartnerNotificationException} so the
 * Kafka consumer retries with exponential backoff. Non-retryable failures (4xx) are logged and
 * skipped.
 *
 * <p>Triggered by the Kafka listener when a matching domain event is received.
 */
@Slf4j
public abstract class NotifyPartnerTemplate extends EventControl<PaymentEvent> {

  @Override
  public final Void execute(PaymentEvent event) {
    Payment payment = event.getPayment();
    String notifyUrl = payment.getNotifyUrl();
    if (notifyUrl == null || notifyUrl.isBlank()) {
      return null;
    }
    if (!payment.getStatus().isTerminal()) {
      return null;
    }
    String body = buildNotificationBody(event);

    HttpClient.Response response;
    try {
      DynamicHttpApi api = httpClient.createHttpApi(DynamicHttpApi.class, notifyUrl);
      response = api.post(body);
    } catch (Exception e) {
      throw new PartnerNotificationException(notifyUrl, e);
    }

    int status = response.getStatusCode();
    if (response.is2xx()) {
      log.info("Partner notified: url={}, status={}", notifyUrl, status);
      return null;
    }

    if (status >= 400 && status < 500) {
      log.warn("Partner notification failed (non-retryable): url={}, status={}", notifyUrl, status);
      return null;
    }

    throw new PartnerNotificationException(notifyUrl, status);
  }

  protected abstract String buildNotificationBody(PaymentEvent event);
}
