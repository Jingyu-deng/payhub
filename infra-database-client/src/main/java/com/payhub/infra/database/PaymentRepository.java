package com.payhub.infra.database;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for {@link PaymentEntity}. */
public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {

  Optional<PaymentEntity> findByOrderId(String orderId);
}
