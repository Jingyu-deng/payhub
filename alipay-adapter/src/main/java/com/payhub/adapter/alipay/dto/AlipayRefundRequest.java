package com.payhub.adapter.alipay.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AlipayRefundRequest {

  private String transactionId;
  private String amount;
  private String action;
}
