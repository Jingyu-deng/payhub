package com.payhub.platform.handler;

import com.payhub.core.exception.DuplicatePaymentException;
import com.payhub.core.exception.JobSchedulingException;
import com.payhub.core.exception.LockAcquisitionException;
import com.payhub.core.exception.PartnerNotificationException;
import com.payhub.core.exception.PaymentProcessingException;
import com.payhub.core.exception.SerializationException;
import com.payhub.core.exception.ServiceNotFoundException;
import com.payhub.infra.monitor.annotation.TrackedError;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @TrackedError("illegal_argument")
  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
    return problem(ex, HttpStatus.BAD_REQUEST);
  }

  @TrackedError("lock_acquisition")
  @ExceptionHandler(LockAcquisitionException.class)
  public ProblemDetail handleLockAcquisition(LockAcquisitionException ex) {
    log.warn("Lock conflict: {}", ex.getMessage());
    return problem(ex, HttpStatus.CONFLICT);
  }

  @TrackedError("duplicate_payment")
  @ExceptionHandler(DuplicatePaymentException.class)
  public ProblemDetail handleDuplicatePayment(DuplicatePaymentException ex) {
    log.warn("Duplicate payment: {}", ex.getMessage());
    return problem(ex, HttpStatus.CONFLICT);
  }

  @TrackedError("service_not_found")
  @ExceptionHandler(ServiceNotFoundException.class)
  public ProblemDetail handleServiceNotFound(ServiceNotFoundException ex) {
    return problem(ex, HttpStatus.NOT_FOUND);
  }

  @TrackedError("partner_notification")
  @ExceptionHandler(PartnerNotificationException.class)
  public ProblemDetail handlePartnerNotification(PartnerNotificationException ex) {
    log.error("Partner notification failed: statusCode={}", ex.getStatusCode(), ex);
    return problem(ex, HttpStatus.BAD_GATEWAY);
  }

  @TrackedError("payment_processing")
  @ExceptionHandler(PaymentProcessingException.class)
  public ProblemDetail handlePaymentProcessing(PaymentProcessingException ex) {
    log.error("Payment processing error", ex);
    return problem(ex, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @TrackedError("infrastructure")
  @ExceptionHandler({JobSchedulingException.class, SerializationException.class})
  public ProblemDetail handleInfrastructureFailure(RuntimeException ex) {
    log.error("Infrastructure error", ex);
    return problem(ex, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @TrackedError("unhandled")
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleFallback(Exception ex) {
    log.error("Unhandled exception", ex);
    return problem(ex, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private ProblemDetail problem(Exception ex, HttpStatus status) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
    detail.setProperty("timestamp", Instant.now().toString());
    return detail;
  }
}
