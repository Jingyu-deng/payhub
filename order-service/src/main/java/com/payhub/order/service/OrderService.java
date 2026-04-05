package com.payhub.order.service;

import com.payhub.common.event.OrderCreatedEvent;
import com.payhub.order.dto.OrderRequest;
import com.payhub.order.entity.Order;
import com.payhub.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
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
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                request.getProductId(),
                request.getQuantity(),
                request.getUserId(),
                System.currentTimeMillis()
        );
        kafkaTemplate.send(TOPIC, orderId, event);
        log.info("Order event sent: {}", event);

        return orderId;
    }
}