package com.payhub.infra.monitor;

import com.payhub.core.enums.PaymentGateway;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/** Custom business metrics for the payment platform. */
@Component
public class PaymentMetrics {

  private final MeterRegistry registry;

  public PaymentMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  // ── Counters ────────────────────────────────────────────────

  public void incrementInitiate() {
    counter("payment.initiate", "gateway", "none").increment();
  }

  public void incrementProcess(PaymentGateway gateway) {
    String gw = gateway != null ? gateway.name().toLowerCase() : "none";
    counter("payment.process", "gateway", gw).increment();
  }

  public void incrementError(String exceptionType) {
    counter("payment.errors", "exception", exceptionType).increment();
  }

  // ── Timer ───────────────────────────────────────────────────

  public void recordDuration(long durationMs) {
    timer("payment.duration").record(durationMs, TimeUnit.MILLISECONDS);
  }

  // ── Helpers ─────────────────────────────────────────────────

  private Counter counter(String name, String... tags) {
    return Counter.builder(name).description("Payment metric").tags(tags).register(registry);
  }

  private Timer timer(String name) {
    return Timer.builder(name).description("Payment duration").register(registry);
  }
}
