package com.payhub.order.dto;

import lombok.Data;

@Data
public class OrderRequest {
  private String productId;
  private Integer quantity;
  private String userId;
}
