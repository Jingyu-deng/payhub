package com.payhub.adapter.wechat.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WechatPayPaymentRequest {

  private String orderId;
  private String amount;
}
