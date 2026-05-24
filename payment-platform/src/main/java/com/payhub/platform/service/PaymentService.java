package com.payhub.platform.service;

import com.payhub.core.controls.CreatePaymentTemplate;
import com.payhub.core.controls.ProcessPaymentTemplate;
import com.payhub.core.dto.PaymentInitiateRequest;
import com.payhub.core.dto.PaymentInitiateResponse;
import com.payhub.core.dto.ProcessPaymentRequest;
import com.payhub.core.dto.ProcessPaymentResponse;
import com.payhub.core.infra.ControlClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentService {

  private final ControlClient controlClient;

  public PaymentService(ControlClient controlClient) {
    this.controlClient = controlClient;
  }

  public PaymentInitiateResponse initiate(PaymentInitiateRequest request) {
    CreatePaymentTemplate template = controlClient.getControl(CreatePaymentTemplate.class);
    return template.execute(request);
  }

  public ProcessPaymentResponse process(ProcessPaymentRequest request) {
    ProcessPaymentTemplate template = controlClient.getControl(ProcessPaymentTemplate.class);
    return template.execute(request);
  }
}
