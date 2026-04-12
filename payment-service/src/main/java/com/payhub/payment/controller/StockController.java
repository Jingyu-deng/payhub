package com.payhub.payment.controller;

import com.payhub.payment.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

  private final StockService stockService;

  @PostMapping("/deduct")
  public String deduct(@RequestParam String productId, @RequestParam int quantity) {
    boolean success = stockService.deductStock(productId, quantity);
    return success ? "Stock deducted" : "Insufficient stock or lock failed";
  }

  @GetMapping("/{productId}")
  public int getStock(@PathVariable String productId) {
    return stockService.getStock(productId);
  }
}
