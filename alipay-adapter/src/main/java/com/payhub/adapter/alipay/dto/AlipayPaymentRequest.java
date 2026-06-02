package com.payhub.adapter.alipay.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AlipayPaymentRequest {

  private String orderId;
  private String amount;
}
