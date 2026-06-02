package com.payhub.adapter.wechat.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WechatPayRefundRequest {

  private String transactionId;
  private String amount;
}
