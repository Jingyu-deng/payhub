package com.payhub.infra.database;

import static org.junit.jupiter.api.Assertions.*;

import com.payhub.core.domain.Payment;
import com.payhub.core.enums.Currency;
import com.payhub.core.enums.PaymentGateway;
import com.payhub.core.enums.PaymentStatus;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(DatabaseClientImpl.class)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
      "spring.datasource.driver-class-name=org.h2.Driver",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
    })
class DatabaseClientImplTest {

  @Autowired private DatabaseClientImpl databaseClient;

  @Autowired private PaymentRepository repository;

  private Payment payment;

  @BeforeEach
  void setUp() {
    repository.deleteAll();

    payment = new Payment();
    payment.setId("pay-1");
    payment.setOrderId("order-1");
    payment.setAmount(new BigDecimal("100.00"));
    payment.setCurrency(Currency.USD);
    payment.setStatus(PaymentStatus.INITIATED);
    payment.setPaymentGateway(PaymentGateway.WECHAT_PAY);
    payment.setTransactionId("txn-123");
    payment.setGatewayResponse("{\"ok\":true}");
    payment.setNotifyUrl("https://partner.example.com/webhook");
    payment.setCheckPgStatusControlJobKey("job-1");
    databaseClient.save(payment);
  }

  @Test
  void shouldSaveAndFindByOrderId() {
    Optional<Payment> found = databaseClient.findByOrderId("order-1");

    assertTrue(found.isPresent());
    assertEquals("pay-1", found.get().getId());
    assertEquals("order-1", found.get().getOrderId());
    assertEquals(new BigDecimal("100.00"), found.get().getAmount());
    assertEquals(Currency.USD, found.get().getCurrency());
    assertEquals(PaymentStatus.INITIATED, found.get().getStatus());
    assertEquals(PaymentGateway.WECHAT_PAY, found.get().getPaymentGateway());
    assertEquals("txn-123", found.get().getTransactionId());
  }

  @Test
  void shouldSaveAndFindByPaymentId() {
    Optional<Payment> found = databaseClient.findByPaymentId("pay-1");

    assertTrue(found.isPresent());
    assertEquals("order-1", found.get().getOrderId());
  }

  @Test
  void shouldReturnEmptyWhenNotFoundByOrderId() {
    Optional<Payment> found = databaseClient.findByOrderId("nonexistent");

    assertTrue(found.isEmpty());
  }

  @Test
  void shouldReturnEmptyWhenNotFoundByPaymentId() {
    Optional<Payment> found = databaseClient.findByPaymentId("nonexistent");

    assertTrue(found.isEmpty());
  }

  @Test
  void shouldUpdateExistingPayment() {
    payment.setStatus(PaymentStatus.COMPLETED);
    payment.setTransactionId("txn-updated");
    databaseClient.save(payment);

    Optional<Payment> found = databaseClient.findByOrderId("order-1");
    assertTrue(found.isPresent());
    assertEquals(PaymentStatus.COMPLETED, found.get().getStatus());
    assertEquals("txn-updated", found.get().getTransactionId());
  }

  @Test
  void shouldPersistNotifyUrlAndJobKey() {
    Optional<Payment> found = databaseClient.findByOrderId("order-1");

    assertTrue(found.isPresent());
    assertEquals("https://partner.example.com/webhook", found.get().getNotifyUrl());
    assertEquals("job-1", found.get().getCheckPgStatusControlJobKey());
  }

  @Test
  void shouldPersistCreatedAt() {
    Optional<Payment> found = databaseClient.findByOrderId("order-1");

    assertTrue(found.isPresent());
    assertNotNull(found.get().getCreatedAt());
  }
}
