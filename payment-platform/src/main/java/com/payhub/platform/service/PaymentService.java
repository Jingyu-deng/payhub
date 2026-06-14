package com.payhub.platform.service;

import com.payhub.core.controls.CreatePaymentTemplate;
import com.payhub.core.controls.ProcessPaymentTemplate;
import com.payhub.core.dto.PaymentInitiateRequest;
import com.payhub.core.dto.PaymentInitiateResponse;
import com.payhub.core.dto.ProcessPaymentRequest;
import com.payhub.core.dto.ProcessPaymentResponse;
import com.payhub.core.infra.ControlClient;
import com.payhub.core.template.CreatePaymentControl;
import com.payhub.core.template.ProcessPaymentControl;
import com.payhub.infra.monitor.annotation.Tracked;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PaymentService {

  private final ControlClient controlClient;

  public PaymentService(ControlClient controlClient) {
    this.controlClient = controlClient;
  }

  @Tracked(operation = "initiate")
  public PaymentInitiateResponse initiate(PaymentInitiateRequest request) {
    CreatePaymentTemplate template = controlClient.getControl(CreatePaymentControl.class);
    return template.execute(request);
  }

  @Tracked(operation = "process")
  public ProcessPaymentResponse process(ProcessPaymentRequest request) {
    ProcessPaymentTemplate template = controlClient.getControl(ProcessPaymentControl.class);
    return template.execute(request);
  }
}
