package com.payhub.infra.database;

import com.payhub.core.domain.Payment;
import com.payhub.core.infra.DatabaseClient;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class DatabaseClientImpl implements DatabaseClient {

  private final Map<String, Payment> store = new ConcurrentHashMap<>();

  @Override
  public void save(Payment payment) {
    store.put(payment.getOrderId(), payment);
  }

  @Override
  public Optional<Payment> findByOrderId(String orderId) {
    return Optional.ofNullable(store.get(orderId));
  }
}
