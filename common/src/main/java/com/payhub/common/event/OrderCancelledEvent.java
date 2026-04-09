package com.payhub.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCancelledEvent {
  private String orderId;
  private String reason;
  private String cancelledBy;
  private Long timestamp;
}
