package com.payhub.core.infra;

import com.payhub.core.domain.Payment;
import java.util.Optional;

/** Port for persisting and retrieving {@link Payment} entities. */
public interface DatabaseClient {

  void save(Payment payment);

  Optional<Payment> findByOrderId(String orderId);
}
