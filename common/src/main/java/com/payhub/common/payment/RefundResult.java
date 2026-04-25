package com.payhub.common.payment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundResult {
  private boolean success;
  private String refundId;
  private String message;
}
