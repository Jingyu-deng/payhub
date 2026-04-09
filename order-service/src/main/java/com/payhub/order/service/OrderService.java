package com.payhub.order.service;

import com.payhub.common.event.OrderCancelledEvent;
import com.payhub.common.event.OrderCreatedEvent;
import com.payhub.order.dto.OrderRequest;
import com.payhub.order.entity.Order;
import com.payhub.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final OrderRepository orderRepository;

  private static final String TOPIC = "order-events";

  @Transactional
  public String createOrder(OrderRequest request) {
    String orderId = UUID.randomUUID().toString();

    // Calculate amount (simple fixed price: 10.00 per quantity)
    BigDecimal amount = BigDecimal.valueOf(10.00 * request.getQuantity());

    // Save order to database
    Order order = new Order();
    order.setId(orderId);
    order.setUserId(request.getUserId());
    order.setProductId(request.getProductId());
    order.setQuantity(request.getQuantity());
    order.setAmount(amount);
    order.setStatus("PENDING");
    orderRepository.save(order);
    log.info("Order saved: {}", order);

    // Send Kafka event
    OrderCreatedEvent event =
        new OrderCreatedEvent(
            orderId,
            request.getProductId(),
            request.getQuantity(),
            request.getUserId(),
            System.currentTimeMillis());
    kafkaTemplate.send(TOPIC, orderId, event);
    log.info("Order event sent: {}", event);

    return orderId;
  }

  @Transactional
  public void cancelOrder(String orderId, String reason, String cancelledBy) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

    if (!"PENDING".equals(order.getStatus())) {
      throw new RuntimeException("Order cannot be cancelled, current status: " + order.getStatus());
    }

    order.setStatus("CANCELLED");
    orderRepository.save(order);

    OrderCancelledEvent event =
        new OrderCancelledEvent(orderId, reason, cancelledBy, System.currentTimeMillis());
    kafkaTemplate.send(TOPIC, orderId, event);
    log.info("Order cancelled event sent: {}", event);
  }
}
