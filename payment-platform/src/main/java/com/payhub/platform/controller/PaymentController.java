package com.payhub.platform.controller;

import com.payhub.core.dto.PaymentInitiateRequest;
import com.payhub.core.dto.PaymentInitiateResponse;
import com.payhub.core.dto.ProcessPaymentRequest;
import com.payhub.core.dto.ProcessPaymentResponse;
import com.payhub.platform.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

  private final PaymentService paymentService;

  public PaymentController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping("/initiate")
  public ResponseEntity<PaymentInitiateResponse> initiate(
      @RequestBody PaymentInitiateRequest request) {
    PaymentInitiateResponse response = paymentService.initiate(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/process")
  public ResponseEntity<ProcessPaymentResponse> process(
      @RequestBody ProcessPaymentRequest request) {
    ProcessPaymentResponse response = paymentService.process(request);
    return ResponseEntity.ok(response);
  }
}
