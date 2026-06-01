package com.payhub.core.properties;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

/** Polling configuration for {@code CheckPaymentStatusTemplate}. */
@Data
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public class CheckPaymentStatusProperties {

  private int pollIntervalSeconds = 30;
  private int maxPollDurationMinutes = 5;
}
