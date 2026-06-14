package com.payhub.infra.database;

import com.payhub.core.domain.Payment;
import com.payhub.core.infra.DatabaseClient;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DatabaseClientImpl implements DatabaseClient {

  private final PaymentRepository repository;

  public DatabaseClientImpl(PaymentRepository repository) {
    this.repository = repository;
  }

  @Override
  public void save(Payment payment) {
    repository.save(toEntity(payment));
  }

  @Override
  public Optional<Payment> findByOrderId(String orderId) {
    return repository.findByOrderId(orderId).map(this::toDomain);
  }

  @Override
  public Optional<Payment> findByPaymentId(String paymentId) {
    return repository.findById(paymentId).map(this::toDomain);
  }

  // ── Mapping helpers ─────────────────────────────────────────────

  private PaymentEntity toEntity(Payment p) {
    var e = new PaymentEntity();
    e.setId(p.getId());
    e.setOrderId(p.getOrderId());
    e.setAmount(p.getAmount());
    e.setCurrency(p.getCurrency());
    e.setStatus(p.getStatus());
    e.setPaymentGateway(p.getPaymentGateway());
    e.setTransactionId(p.getTransactionId());
    e.setGatewayResponse(p.getGatewayResponse());
    e.setNotifyUrl(p.getNotifyUrl());
    e.setCheckPgStatusControlJobKey(p.getCheckPgStatusControlJobKey());
    e.setCreatedAt(p.getCreatedAt());
    return e;
  }

  private Payment toDomain(PaymentEntity e) {
    var p = new Payment();
    p.setId(e.getId());
    p.setOrderId(e.getOrderId());
    p.setAmount(e.getAmount());
    p.setCurrency(e.getCurrency());
    p.setStatus(e.getStatus());
    p.setPaymentGateway(e.getPaymentGateway());
    p.setTransactionId(e.getTransactionId());
    p.setGatewayResponse(e.getGatewayResponse());
    p.setNotifyUrl(e.getNotifyUrl());
    p.setCheckPgStatusControlJobKey(e.getCheckPgStatusControlJobKey());
    return p;
  }
}
