package com.payhub.core.adapters;

import com.payhub.core.domain.PaymentResult;
import com.payhub.core.domain.PaymentStatusResult;
import com.payhub.core.domain.RefundResult;
import com.payhub.core.enums.Currency;
import com.payhub.core.enums.PaymentGateway;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Adapter SPI contract for external payment providers. Implementations must be registered in
 * META-INF/services/ and depend ONLY on core — never on infra or Spring.
 */
public interface Adapter {

  PaymentResult processPayment(
      String orderId, BigDecimal amount, Currency currency, Map<String, String> params);

  RefundResult refund(String transactionId, BigDecimal amount);

  PaymentStatusResult checkPaymentStatus(String transactionId);

  PaymentGateway getGateway();
}
