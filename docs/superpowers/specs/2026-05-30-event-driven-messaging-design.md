# Event-Driven Messaging with Kafka — Design Spec

**Goal:** Replace the SLF4J-stub EventPublisher with real Kafka pub/sub and add an EventListener/EventControl mechanism so that domain events trigger downstream business logic (starting with partner notification on payment completion).

**Architecture:** `PaymentEvent` is serialized as JSON to a Kafka topic (`payment-events`). A `@KafkaListener` deserializes and dispatches to SPI-discovered `EventControl<I>` implementations matched by event type. Each `EventControl` extends `ControlInjector` and receives all infra ports, including a new `HttpClient` field for outbound HTTP calls.

**Tech Stack:** Spring Kafka (`KafkaTemplate`, `@KafkaListener`), Jackson JSON serialization, Java SPI (`ServiceLoader`), OkHttp for partner webhook delivery.

---

## 1. Data Model Changes

### 1.1 `PaymentInitiateRequest` — new field

```java
@Data
public class PaymentInitiateRequest {
    private String orderId;
    private BigDecimal amount;
    private Currency currency;
    private String notifyUrl;  // NEW
}
```

### 1.2 `Payment` — new field

```java
public class Payment {
    // ... existing fields ...
    private String notifyUrl;  // NEW
}
```

### 1.3 `PaymentEvent` — new field

```java
public class PaymentEvent {
    private final PaymentStatus type;
    private final String orderId;
    private final String paymentId;
    private final PaymentGateway gateway;
    private final String transactionId;
    private final long timestamp;
    private final String notifyUrl;  // NEW
}
```

---

## 2. New Port Interface: `EventListener` (`core-entities/.../infra/`)

```java
public interface EventListener {
    void onEvent(PaymentEvent event);
}
```

Single method. Called by `EventListenerImpl` (Kafka consumer). Dispatches to the appropriate `EventControl`.

---

## 3. New Abstract Base: `EventControl<I>` (`core-entities/.../controls/base/`)

```java
public abstract class EventControl<I> extends ControlInjector<I, Void> {
    /** The PaymentStatus this control handles (e.g., COMPLETED). */
    public abstract PaymentStatus getHandledEventType();
}
```

Extends `ControlInjector<I, Void>` (event-driven flow has no return value — fire-and-forget). Each concrete subclass declares which `PaymentStatus` it handles.

Registered via SPI in `META-INF/services/com.payhub.core.controls.base.Control` (same as other controls).

---

## 4. `ControlInjector` — Add `HttpClient`

`ControlInjector` gains a new `protected HttpClient httpClient` field (with `@Getter @Setter` from Lombok). This allows any control, including event-driven ones, to make outbound HTTP calls.

`ControlClientImpl` must also inject `HttpClient`.

---

## 5. Kafka Implementation (`infra-message-client`)

### 5.1 `EventPublisherImpl` — rewrite

Replace SLF4J stub. Inject `KafkaTemplate<String, String>`. Serialize `PaymentEvent` via `JsonUtils.toJson()` and send to topic `payment-events`.

### 5.2 `EventListenerImpl` — new class

`@Component` implementing `EventListener`:

1. Wraps a `@KafkaListener(topics = "payment-events")` method.
2. Deserializes JSON → `PaymentEvent` via `JsonUtils.fromJson()`.
3. Looks up all `EventControl` beans from `ApplicationContext.getBeansOfType(EventControl.class)`.
4. Filters by `getHandledEventType()` matching the event's type.
5. For each matching control, injects all `ControlInjector` dependencies (same wiring pattern as `ControlClientImpl`: sets `adapterClient`, `databaseClient`, `idempotencyClient`, `eventPublisher`, `schedulerClient`, `controlClient`, `httpClient`).
6. Calls `eventControl.execute(event)`.

For now the `EventListener` port is consumed directly; the `EventListenerImpl` IS the Kafka listener that implements it. If we later add a second dispatch mechanism (e.g., in-process direct dispatch without Kafka), we can add another `EventListener` impl.

**Topic:** `payment-events` (single topic, keyed by paymentId for ordering). Consumer group: `payhub-consumer`.

---

## 6. Kafka Config (`payment-platform`)

### 6.1 `PayHubKafkaConfig` — new `@Configuration`

```java
@Configuration
public class PayHubKafkaConfig {
    @Bean
    public KafkaListenerContainerFactory<?> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        return factory;
    }
}
```

### 6.2 `application.yml` — add Kafka properties

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

String serialization on both sides — JSON conversion is explicit in our code via `JsonUtils`.

---

## 7. Event Publishing in Templates

### 7.1 `CheckPaymentStatusTemplate` — add event publishing

Currently this template does NOT publish events when status becomes terminal. We add `eventPublisher.publish(...)` when `isTerminal()` is true, including `notifyUrl` from the payment entity. This is the trigger for `NotifyPartnerControl`.

### 7.2 `CreatePaymentTemplate` and `ProcessPaymentTemplate` — update event construction

Include `payment.getNotifyUrl()` in the `PaymentEvent` constructor calls.

---

## 8. `NotifyPartnerControl` (`core-applications`)

```java
public class NotifyPartnerControl extends EventControl<PaymentEvent> {

    @Override
    public PaymentStatus getHandledEventType() {
        return PaymentStatus.COMPLETED;
    }

    @Override
    public Void execute(PaymentEvent event) {
        if (event.getNotifyUrl() == null || event.getNotifyUrl().isBlank()) {
            return null;  // no webhook configured, skip
        }
        String body = JsonUtils.toJson(event);
        HttpClient.Response response = httpClient.post(
                event.getNotifyUrl(),
                Map.of("Content-Type", "application/json"),
                body);
        log.info("Partner notified: url={}, status={}", event.getNotifyUrl(), response.getStatusCode());
        return null;
    }
}
```

Registered in `META-INF/services/com.payhub.core.controls.base.Control`.

---

## 9. Files Summary

| Action | File |
|---|---|
| Modify | `core-entities/.../dto/PaymentInitiateRequest.java` |
| Modify | `core-entities/.../domain/Payment.java` |
| Modify | `core-entities/.../domain/PaymentEvent.java` |
| Modify | `core-entities/.../controls/base/ControlInjector.java` |
| New | `core-entities/.../controls/base/EventControl.java` |
| New | `core-entities/.../infra/EventListener.java` |
| Modify | `core-entities/.../controls/CreatePaymentTemplate.java` |
| Modify | `core-entities/.../controls/ProcessPaymentTemplate.java` |
| Modify | `core-entities/.../controls/CheckPaymentStatusTemplate.java` |
| Rewrite | `infra-message-client/.../EventPublisherImpl.java` |
| New | `infra-message-client/.../EventListenerImpl.java` |
| Modify | `infra-runtime/.../ControlClientImpl.java` |
| New | `payment-platform/.../config/PayHubKafkaConfig.java` |
| Modify | `payment-platform/.../resources/application.yml` |
| New | `core-applications/.../NotifyPartnerControl.java` |
| Modify | `core-applications/.../resources/META-INF/services/com.payhub.core.controls.base.Control` |
