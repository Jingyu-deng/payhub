package com.payhub.platform;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.payhub.core.domain.Payment;
import com.payhub.core.dto.PaymentInitiateRequest;
import com.payhub.core.dto.PaymentInitiateResponse;
import com.payhub.core.enums.Currency;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.infra.AdapterClient;
import com.payhub.core.infra.DatabaseClient;
import com.payhub.core.infra.EventPublisher;
import com.payhub.core.infra.IdempotencyClient;
import com.payhub.core.infra.SchedulerClient;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test verifies the full stack — HTTP, controller, service, template, and database —
 * wired together with H2. External infrastructure (Redis, Kafka, Quartz) is mocked so the test runs
 * without external services.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PaymentPlatformApplicationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private DatabaseClient databaseClient;

  @MockBean private IdempotencyClient idempotencyClient;
  @MockBean private EventPublisher eventPublisher;
  @MockBean private SchedulerClient schedulerClient;
  @MockBean private AdapterClient adapterClient;

  @BeforeEach
  void setUp() {
    // Let the template acquire the lock and skip duplicate checks
    when(idempotencyClient.acquireLock(anyString(), anyLong(), anyLong())).thenReturn(true);
    when(idempotencyClient.isAlreadyProcessed(anyString())).thenReturn(false);
  }

  @Test
  void shouldInitiatePaymentAndPersistToDatabase() {
    // Given: a payment initiation request
    PaymentInitiateRequest request = new PaymentInitiateRequest();
    request.setOrderId("order-integration-test");
    request.setAmount(new BigDecimal("299.99"));
    request.setCurrency(Currency.USD);
    request.setNotifyUrl("https://example.com/webhook");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<PaymentInitiateRequest> entity = new HttpEntity<>(request, headers);

    // When: the initiate endpoint is called
    ResponseEntity<PaymentInitiateResponse> response =
        restTemplate.postForEntity(
            "http://localhost:" + port + "/api/payments/initiate",
            entity,
            PaymentInitiateResponse.class);

    // Then: HTTP 200 with a valid response
    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertNotNull(response.getBody().getPaymentId());
    assertEquals(PaymentStatus.INITIATED, response.getBody().getStatus());

    // And: payment was actually persisted to the database
    Optional<Payment> saved = databaseClient.findByOrderId("order-integration-test");
    assertTrue(saved.isPresent(), "Payment should be persisted to H2");
    assertEquals(response.getBody().getPaymentId(), saved.get().getId());
    assertEquals(new BigDecimal("299.99"), saved.get().getAmount());
    assertEquals(Currency.USD, saved.get().getCurrency());
    assertEquals("https://example.com/webhook", saved.get().getNotifyUrl());
    assertNotNull(saved.get().getCreatedAt());
  }

  @Test
  void shouldRejectDuplicateOrderId() {
    // Given: a completed first payment
    PaymentInitiateRequest request = new PaymentInitiateRequest();
    request.setOrderId("order-dup-test");
    request.setAmount(new BigDecimal("50.00"));
    request.setCurrency(Currency.USD);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    // First request succeeds
    restTemplate.postForEntity(
        "http://localhost:" + port + "/api/payments/initiate",
        new HttpEntity<>(request, headers),
        PaymentInitiateResponse.class);

    // Second request: simulate that the first was already processed
    when(idempotencyClient.isAlreadyProcessed(anyString())).thenReturn(true);

    // When/Then: second request should fail with 409
    ResponseEntity<String> duplicateResponse =
        restTemplate.postForEntity(
            "http://localhost:" + port + "/api/payments/initiate",
            new HttpEntity<>(request, headers),
            String.class);

    assertEquals(409, duplicateResponse.getStatusCode().value());
  }
}
