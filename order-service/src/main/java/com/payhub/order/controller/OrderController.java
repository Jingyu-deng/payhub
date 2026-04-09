package com.payhub.order.controller;

import com.payhub.order.dto.OrderRequest;
import com.payhub.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
  private final OrderService orderService;

  @PostMapping
  public ResponseEntity<String> createOrder(@RequestBody OrderRequest request) {
    String orderId = orderService.createOrder(request);
    return ResponseEntity.ok(orderId);
  }

  @DeleteMapping("/{orderId}")
  public ResponseEntity<String> cancelOrder(
      @PathVariable String orderId,
      @RequestParam(defaultValue = "User requested") String reason,
      @RequestParam(defaultValue = "customer") String cancelledBy) {
    orderService.cancelOrder(orderId, reason, cancelledBy);
    return ResponseEntity.ok("Order cancellation event sent for " + orderId);
  }
}
