package com.payhub.payment.consumer;

import com.payhub.common.event.OrderCancelledEvent;
import com.payhub.common.event.OrderCreatedEvent;
import com.payhub.common.exception.PaymentProcessingException;
import com.payhub.common.payment.PaymentGateway;
import com.payhub.common.payment.PaymentResult;
import com.payhub.payment.entity.Payment;
import com.payhub.payment.repository.PaymentRepository;
import com.payhub.payment.service.IdempotencyService;
import com.payhub.payment.service.PaymentRouter;
import com.payhub.payment.service.StockService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(topics = "order-events", groupId = "payment-group")
public class OrderConsumer {

  private final PaymentRepository paymentRepository;
  private final IdempotencyService idempotencyService;
  private final StockService stockService;
  private final RedissonClient redissonClient; // for idempotency lock
  private final PaymentRouter paymentRouter;

  @KafkaHandler
  @Transactional
  public void handleOrderCreated(OrderCreatedEvent event) {
    String orderId = event.getOrderId();
    String idempotencyLockKey = "lock:idempotency:" + orderId;
    RLock idempotencyLock = redissonClient.getLock(idempotencyLockKey);

    try {
      // Acquire idempotency lock (wait up to 5 seconds, hold for 10 seconds)
      if (!idempotencyLock.tryLock(5, TimeUnit.SECONDS)) {
        log.warn("Could not acquire idempotency lock for order {}", orderId);
        return;
      }

      // Duplicate check (now protected by lock)
      if (idempotencyService.isAlreadyProcessed(orderId)) {
        log.warn("Order {} already processed, skipping", orderId);
        return;
      }

      // Deduct stock (stock service has its own distributed lock on productId)
      boolean deducted = stockService.deductStock(event.getProductId(), event.getQuantity());
      if (!deducted) {
        throw new PaymentProcessingException(
            String.format(
                "Insufficient stock for product %s (requested: %d)",
                event.getProductId(), event.getQuantity()));
      }

      // Route to payment gateway
      PaymentGateway gateway = paymentRouter.route(event.getUserId(), event.getProductId());
      BigDecimal amount = BigDecimal.valueOf(10.00 * event.getQuantity());
      PaymentResult result = gateway.processPayment(orderId, amount, "CNY", Map.of());

      if (!result.isSuccess()) {
        throw new PaymentProcessingException("Payment failed: " + result.getMessage());
      }

      // Save payment
      Payment payment = new Payment();
      payment.setId(UUID.randomUUID().toString());
      payment.setOrderId(orderId);
      payment.setAmount(amount);
      payment.setStatus("SUCCESS");
      payment.setPaymentMethod(gateway.getGatewayName());
      payment.setTransactionId(result.getTransactionId());
      paymentRepository.save(payment);

      // Mark as processed (idempotency)
      idempotencyService.markAsProcessed(orderId);
      log.info("Payment recorded for order: {} via {}", orderId, gateway.getGatewayName());

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.error("Interrupted while acquiring idempotency lock for order {}", orderId, e);
    } finally {
      if (idempotencyLock.isHeldByCurrentThread()) {
        idempotencyLock.unlock();
      }
    }
  }

  @KafkaHandler
  public void handleOrderCancelled(OrderCancelledEvent event) {
    log.info("Order cancelled: {}", event);
    // Example: release any pending payment authorizations, update status
  }
}
