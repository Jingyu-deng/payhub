package com.payhub.core.domain;

import lombok.Value;

@Value
public class RefundResult {

  boolean success;
  String refundId;
  String message;
}
