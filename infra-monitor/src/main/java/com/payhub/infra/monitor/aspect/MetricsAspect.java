package com.payhub.infra.monitor.aspect;

import com.payhub.core.dto.ProcessPaymentRequest;
import com.payhub.core.enums.PaymentGateway;
import com.payhub.infra.monitor.PaymentMetrics;
import com.payhub.infra.monitor.annotation.Tracked;
import com.payhub.infra.monitor.annotation.TrackedError;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * Drives {@link PaymentMetrics} via annotated service and exception-handler methods so business
 * code never references monitoring classes directly.
 */
@Aspect
@Component
public class MetricsAspect {

  private final PaymentMetrics metrics;

  public MetricsAspect(PaymentMetrics metrics) {
    this.metrics = metrics;
  }

  /** Records an operation counter and wall-clock duration around {@code @Tracked} methods. */
  @Around("@annotation(tracked)")
  public Object trackOperation(ProceedingJoinPoint joinPoint, Tracked tracked) throws Throwable {
    String operation = tracked.operation();

    if ("initiate".equals(operation)) {
      metrics.incrementInitiate();
    } else if ("process".equals(operation)) {
      metrics.incrementProcess(extractGateway(joinPoint));
    }

    long start = System.currentTimeMillis();
    try {
      return joinPoint.proceed();
    } finally {
      metrics.recordDuration(System.currentTimeMillis() - start);
    }
  }

  /** Increments the error counter before the {@code @TrackedError} handler creates its response. */
  @Before("@annotation(trackedError)")
  public void trackError(TrackedError trackedError) {
    metrics.incrementError(trackedError.value());
  }

  private static PaymentGateway extractGateway(ProceedingJoinPoint joinPoint) {
    for (Object arg : joinPoint.getArgs()) {
      if (arg instanceof ProcessPaymentRequest request) {
        return request.getGatewayName();
      }
    }
    return null;
  }
}
