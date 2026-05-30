# Event-Driven Messaging with Kafka — Design Spec

**Goal:** Replace the SLF4J-stub EventPublisher with real Kafka pub/sub and add an EventListener/EventControl mechanism so that domain events trigger downstream business logic (starting with partner notification on payment completion).

**Architecture:** `PaymentEvent` wraps the `Payment` aggregate, is encrypted via `EncryptionClient`, and published to a Kafka topic (`payment-events`). A `@KafkaListener` decrypts and dispatches to `EventControl` implementations via a dedicated `ControlClient.getEventControls()` method. Each `EventControl` extends `ControlInjector` and receives all infra ports.

**Tech Stack:** Spring Kafka (`KafkaTemplate`, `@KafkaListener`), Jackson JSON, Java SPI (`ServiceLoader`), OkHttp for partner webhook delivery.

**MQ Switchability:** The `EventPublisher` and `EventListener` ports live in `core-entities`. Kafka-specific code is entirely contained in `infra-message-client` implementations. Switching to RabbitMQ or another broker requires only swapping those two `*Impl` classes — zero changes to ports, controls, or templates.

---

## 1. Data Model Changes

### 1.1 `PaymentInitiateRequest` — new field

```java
@Data
public class PaymentInitiateRequest {
    private String orderId;
    private BigDecimal amount;
    private Currency currency;
    private String notifyUrl;  // NEW: partner webhook URL
}
```

### 1.2 `Payment` — new field

```java
public class Payment {
    // ... existing fields (id, orderId, amount, currency, status, paymentGateway,
    //   transactionId, gatewayResponse, checkPgStatusControlJobKey, createdAt) ...
    private String notifyUrl;  // NEW
}
```

### 1.3 `PaymentEvent` — restructured to embed the Payment aggregate

```java
@Data
@AllArgsConstructor
public class PaymentEvent {
    private final PaymentStatus type;    // the status transition this event represents
    private final Payment payment;       // the full aggregate
    private final long timestamp;        // epoch millis
}
```

No more individual field duplication. Consumers get the full Payment aggregate.

---

## 2. New Port Interface: `EncryptionClient` (`core-entities/.../infra/`)

```java
public interface EncryptionClient {
    String encrypt(String plaintext);
    String decrypt(String ciphertext);
}
```

Used by `EventPublisherImpl` before publishing and `EventListenerImpl` after receiving. Implementation is a no-op stub for now (returns input unchanged) — see section 5.3.

---

## 3. New Port Interface: `EventListener` (`core-entities/.../infra/`)

```java
public interface EventListener {
    void onEvent(PaymentEvent event);
}
```

`EventListenerImpl` (the Kafka consumer) calls this, then dispatches to `EventControl` instances via `ControlClient.getEventControls()`.

---

## 4. New Abstract Base: `EventControl<I>` (`core-entities/.../controls/base/`)

```java
public abstract class EventControl<I> extends ControlInjector<I, Void> {
    public abstract PaymentStatus getHandledEventType();
}
```

Extends `ControlInjector<I, Void>` — event-driven flow has no return value (fire-and-forget). Each concrete subclass declares which `PaymentStatus` it reacts to.

Registered via SPI in `META-INF/services/com.payhub.core.controls.base.Control` (same file as other controls).

---

## 5. `ControlInjector` — Add `HttpClient`

`ControlInjector` gains: `protected HttpClient httpClient` (with Lombok `@Getter @Setter`).

`ControlClientImpl` also receives `HttpClient` via constructor and sets it on each control.

---

## 6. `ControlClient` — New Method

New method on the port interface (`core-entities/.../infra/ControlClient.java`):

```java
List<EventControl<PaymentEvent>> getEventControls(PaymentStatus eventType);
```

`ControlClientImpl` implementation:
1. Looks up all `EventControl` beans from `ApplicationContext`.
2. Filters by `getHandledEventType()` matching `eventType`.
3. Wires each matching control via `ControlInjector` setters (same wiring pattern as `getControl()`).
4. Returns the list.

`EventListenerImpl` calls this instead of doing its own wiring — keeps `EventListenerImpl` dedicated to Kafka message handling.

---

## 7. Kafka Implementation (`infra-message-client`)

### 7.1 `EventPublisherImpl` — rewrite

Replace SLF4J stub. Inject `KafkaTemplate<String, String>` and `EncryptionClient`.

Flow:
1. Serialize `PaymentEvent` → JSON via `JsonUtils.toJson()`.
2. Encrypt via `encryptionClient.encrypt(json)`.
3. Send to Kafka topic `payment-events`, keyed by `event.getPayment().getId()`.

### 7.2 `EventListenerImpl` — new class

`@Component` implementing `EventListener`:

1. `@KafkaListener(topics = "payment-events")` method receives raw message.
2. Decrypt via `encryptionClient.decrypt(raw)`.
3. Deserialize JSON → `PaymentEvent` via `JsonUtils.fromJson()`.
4. Calls `controlClient.getEventControls(event.getType())` to get wired `EventControl` list.
5. Iterates and calls `control.execute(event)` on each.

### 7.3 `EncryptionClientImpl` — new no-op stub

`@Component` implementing `EncryptionClient`. Both `encrypt()` and `decrypt()` return the input unchanged. Real encryption comes later when the implementation is built.

### 7.4 Kafka Topic & Consumer Group

- **Topic:** `payment-events`
- **Consumer group:** `payhub-consumer`
- **Key:** `payment.getId()` (keeps events for the same payment ordered)

---

## 8. Kafka Config (`payment-platform`)

### 8.1 `kafka.yml` — new file in `payment-platform/src/main/resources/`

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: payhub-consumer
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
```

### 8.2 `PayHubKafkaConfig` — new `@Configuration`

```java
@Configuration
@PropertySource(value = "classpath:kafka.yml", factory = YamlPropertySourceFactory.class)
public class PayHubKafkaConfig {
}
```

Spring Boot auto-configures `KafkaTemplate`, `ConsumerFactory`, and `KafkaListenerContainerFactory` from `spring.kafka.*` properties. No manual bean definitions needed. `YamlPropertySourceFactory` is the existing utility in `infra-common`.

String serialization on both sides — JSON conversion and encryption are explicit in our code via `JsonUtils` and `EncryptionClient`.

---

## 9. Event Publishing in Templates

### 9.1 `CheckPaymentStatusTemplate` — add event publishing

This template currently does NOT publish events when status becomes terminal. Add `eventPublisher.publish(...)` after `isTerminal()` is true, before returning. The event wraps the `Payment` aggregate, so downstream controls (like `NotifyPartnerControl`) have all the data.

### 9.2 `CreatePaymentTemplate` and `ProcessPaymentTemplate` — update event construction

Use the new `PaymentEvent(type, payment, timestamp)` constructor. No need to extract individual fields.

---

## 10. `NotifyPartnerControl` (`core-applications`)

```java
public class NotifyPartnerControl extends EventControl<PaymentEvent> {

    @Override
    public PaymentStatus getHandledEventType() {
        return PaymentStatus.COMPLETED;
    }

    @Override
    public Void execute(PaymentEvent event) {
        Payment payment = event.getPayment();
        String notifyUrl = payment.getNotifyUrl();
        if (notifyUrl == null || notifyUrl.isBlank()) {
            return null;
        }
        String body = JsonUtils.toJson(event);
        HttpClient.Response response = httpClient.post(
                notifyUrl,
                Map.of("Content-Type", "application/json"),
                body);
        log.info("Partner notified: url={}, status={}", notifyUrl, response.getStatusCode());
        return null;
    }
}
```

Registered in `META-INF/services/com.payhub.core.controls.base.Control`.

---

## 11. Files Summary

| Action | File |
|---|---|
| Modify | `core-entities/.../dto/PaymentInitiateRequest.java` |
| Modify | `core-entities/.../domain/Payment.java` |
| Modify | `core-entities/.../domain/PaymentEvent.java` |
| Modify | `core-entities/.../controls/base/ControlInjector.java` |
| New | `core-entities/.../controls/base/EventControl.java` |
| New | `core-entities/.../infra/EventListener.java` |
| New | `core-entities/.../infra/EncryptionClient.java` |
| Modify | `core-entities/.../infra/ControlClient.java` |
| Modify | `core-entities/.../controls/CreatePaymentTemplate.java` |
| Modify | `core-entities/.../controls/ProcessPaymentTemplate.java` |
| Modify | `core-entities/.../controls/CheckPaymentStatusTemplate.java` |
| Rewrite | `infra-message-client/.../EventPublisherImpl.java` |
| New | `infra-message-client/.../EventListenerImpl.java` |
| New | `infra-message-client/.../EncryptionClientImpl.java` |
| Modify | `infra-runtime/.../ControlClientImpl.java` |
| New | `payment-platform/.../config/PayHubKafkaConfig.java` |
| New | `payment-platform/.../resources/kafka.yml` |
| New | `core-applications/.../NotifyPartnerControl.java` |
| Modify | `core-applications/.../resources/META-INF/services/com.payhub.core.controls.base.Control` |
