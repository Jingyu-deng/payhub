package com.payhub.payment.consumer;

import com.payhub.common.event.OrderCreatedEvent;
import com.payhub.payment.entity.Payment;
import com.payhub.payment.repository.PaymentRepository;
import com.payhub.payment.service.IdempotencyService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConsumer {

    private final PaymentRepository paymentRepository;
    private final IdempotencyService idempotencyService;

    @KafkaListener(topics = "order-events", groupId = "payment-group")
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        String orderId = event.getOrderId();

        // Duplicate check
        if (idempotencyService.isAlreadyProcessed(orderId)) {
            log.warn("Order {} already processed, skipping", orderId);
            return;
        }

        log.info("Processing order event: {}", event);

        // Simulate payment processing (always success)
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID().toString());
        payment.setOrderId(event.getOrderId());
        // In a real scenario, amount would come from order details; here we recalc
        BigDecimal amount = BigDecimal.valueOf(10.00 * event.getQuantity());
        payment.setAmount(amount);
        payment.setStatus("SUCCESS");
        payment.setPaymentMethod("MOCK");
        paymentRepository.save(payment);
       
        // Mark as processed
        idempotencyService.markAsProcessed(orderId);
        log.info("Payment recorded for order: {}", orderId);
    }
}