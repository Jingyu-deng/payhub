# Event-Driven Messaging with Kafka — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the SLF4J-stub EventPublisher with real Kafka pub/sub, add EventListener/EventControl mechanism, and build NotifyPartnerControl for partner webhook delivery on payment completion.

**Architecture:** `PaymentEvent` wraps the `Payment` aggregate, encrypted via `EncryptionClient` then published to Kafka (`payment-events` topic). A `@KafkaListener` decrypts, deserializes, and dispatches to `EventControl` beans via `ControlClient.getEventControls()`. Kafka config is loaded from `kafka.yml` via `YamlPropertySourceFactory` and Spring Boot auto-configures the rest.

**Tech Stack:** Spring Kafka (`KafkaTemplate`, `@KafkaListener`), Jackson JSON via `JsonUtils`, Java SPI (`ServiceLoader`), OkHttp for partner webhook, JUnit 5 + Mockito for tests.

---

### Task 1: Data Model — Add `notifyUrl` and restructure `PaymentEvent`

**Files:**
- Modify: `e:\payhub\core-entities\src\main\java\com\payhub\core\dto\PaymentInitiateRequest.java`
- Modify: `e:\payhub\core-entities\src\main\java\com\payhub\core\domain\Payment.java`
- Modify: `e:\payhub\core-entities\src\main\java\com\payhub\core\domain\PaymentEvent.java`
- Create: `e:\payhub\core-entities\src\test\java\com\payhub\core\domain\PaymentEventTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.payhub.core.domain;

import static org.junit.jupiter.api.Assertions.*;

import com.payhub.core.enums.Currency;
import com.payhub.core.enums.PaymentStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentEventTest {

    @Test
    void shouldSerializeAndDeserializeViaJsonUtils() {
        Payment payment = new Payment();
        payment.setId("pay-123");
        payment.setOrderId("order-456");
        payment.setAmount(new BigDecimal("100.00"));
        payment.setCurrency(Currency.CNY);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setNotifyUrl("https://partner.example.com/webhook");

        PaymentEvent event = new PaymentEvent(PaymentStatus.COMPLETED, payment, 1717000000000L);

        String json = com.payhub.core.utils.JsonUtils.toJson(event);
        PaymentEvent restored = com.payhub.core.utils.JsonUtils.fromJson(json, PaymentEvent.class);

        assertEquals(PaymentStatus.COMPLETED, restored.getType());
        assertEquals("pay-123", restored.getPayment().getId());
        assertEquals("order-456", restored.getPayment().getOrderId());
        assertEquals("https://partner.example.com/webhook", restored.getPayment().getNotifyUrl());
        assertEquals(1717000000000L, restored.getTimestamp());
    }

    @Test
    void shouldSerializeAndDeserializeMinimalEvent() {
        Payment payment = new Payment();
        payment.setId("pay-min");
        payment.setOrderId("order-min");
        payment.setStatus(PaymentStatus.INITIATED);

        PaymentEvent event = new PaymentEvent(PaymentStatus.INITIATED, payment, System.currentTimeMillis());

        String json = com.payhub.core.utils.JsonUtils.toJson(event);
        PaymentEvent restored = com.payhub.core.utils.JsonUtils.fromJson(json, PaymentEvent.class);

        assertEquals(PaymentStatus.INITIATED, restored.getType());
        assertEquals("pay-min", restored.getPayment().getId());
        assertNull(restored.getPayment().getNotifyUrl());
        assertTrue(restored.getTimestamp() > 0);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core-entities:test --tests "com.payhub.core.domain.PaymentEventTest"`
Expected: COMPILATION ERROR — `PaymentEvent` constructor `(PaymentStatus, Payment, long)` does not exist yet.

- [ ] **Step 3: Implement data model changes**

In `PaymentInitiateRequest.java` — add `notifyUrl` field:
```java
package com.payhub.core.dto;

import com.payhub.core.enums.Currency;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class PaymentInitiateRequest {

    private String orderId;
    private BigDecimal amount;
    private Currency currency;
    private String notifyUrl;
}
```

In `Payment.java` — add `notifyUrl` field after `gatewayResponse`:
```java
// Add this field (after gatewayResponse, before checkPgStatusControlJobKey):
private String notifyUrl;
```

In `PaymentEvent.java` — restructure to embed Payment aggregate:
```java
package com.payhub.core.domain;

import com.payhub.core.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentEvent {

    private final PaymentStatus type;
    private final Payment payment;
    private final long timestamp;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :core-entities:test --tests "com.payhub.core.domain.PaymentEventTest"`
Expected: PASS — 2 tests pass.

- [ ] **Step 5: Run full core-entities test suite to check for regressions**

Run: `./gradlew :core-entities:test`
Expected: PASS — no regressions.

- [ ] **Step 6: Commit**

```bash
git add core-entities/src/main/java/com/payhub/core/dto/PaymentInitiateRequest.java core-entities/src/main/java/com/payhub/core/domain/Payment.java core-entities/src/main/java/com/payhub/core/domain/PaymentEvent.java core-entities/src/test/java/com/payhub/core/domain/PaymentEventTest.java
git commit -m "feat: add notifyUrl to payment model and restructure PaymentEvent to embed Payment aggregate"
```

---

### Task 2: `EncryptionClient` Port + No-Op Stub

**Files:**
- Create: `e:\payhub\core-entities\src\main\java\com\payhub\core\infra\EncryptionClient.java`
- Create: `e:\payhub\infra-message-client\src\main\java\com\payhub\infra\event\EncryptionClientImpl.java`
- Create: `e:\payhub\infra-message-client\src\test\java\com\payhub\infra\event\EncryptionClientImplTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.payhub.infra.event;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EncryptionClientImplTest {

    private EncryptionClientImpl encryptionClient;

    @BeforeEach
    void setUp() {
        encryptionClient = new EncryptionClientImpl();
    }

    @Test
    void shouldEncryptAndDecryptAsIdentity() {
        String plaintext = "{\"type\":\"COMPLETED\",\"payment\":{\"id\":\"pay-123\"}}";

        String encrypted = encryptionClient.encrypt(plaintext);
        String decrypted = encryptionClient.decrypt(encrypted);

        assertEquals(plaintext, encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void shouldHandleEmptyString() {
        assertEquals("", encryptionClient.encrypt(""));
        assertEquals("", encryptionClient.decrypt(""));
    }

    @Test
    void shouldHandleNull() {
        assertNull(encryptionClient.encrypt(null));
        assertNull(encryptionClient.decrypt(null));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :infra-message-client:test --tests "com.payhub.infra.event.EncryptionClientImplTest"`
Expected: COMPILATION ERROR — `EncryptionClientImpl` class does not exist.

- [ ] **Step 3: Create `EncryptionClient` port interface**

```java
package com.payhub.core.infra;

/**
 * Infrastructure interface for encrypting/decrypting data at rest and in transit.
 */
public interface EncryptionClient {

    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
```

- [ ] **Step 4: Create `EncryptionClientImpl` no-op stub**

```java
package com.payhub.infra.event;

import com.payhub.core.infra.EncryptionClient;
import org.springframework.stereotype.Component;

/**
 * No-op stub that returns input unchanged. Real encryption (AES/KMS) comes later when the
 * implementation is built.
 */
@Component
public class EncryptionClientImpl implements EncryptionClient {

    @Override
    public String encrypt(String plaintext) {
        return plaintext;
    }

    @Override
    public String decrypt(String ciphertext) {
        return ciphertext;
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :infra-message-client:test --tests "com.payhub.infra.event.EncryptionClientImplTest"`
Expected: PASS — 3 tests pass.

- [ ] **Step 6: Commit**

```bash
git add core-entities/src/main/java/com/payhub/core/infra/EncryptionClient.java infra-message-client/src/main/java/com/payhub/infra/event/EncryptionClientImpl.java infra-message-client/src/test/java/com/payhub/infra/event/EncryptionClientImplTest.java
git commit -m "feat: add EncryptionClient port and no-op stub implementation"
```

---

### Task 3: `EventControl` Base Class + `EventListener` Port + `ControlInjector` HttpClient

**Files:**
- Create: `e:\payhub\core-entities\src\main\java\com\payhub\core\controls\base\EventControl.java`
- Create: `e:\payhub\core-entities\src\main\java\com\payhub\core\infra\EventListener.java`
- Modify: `e:\payhub\core-entities\src\main\java\com\payhub\core\controls\base\ControlInjector.java`
- Create: `e:\payhub\core-entities\src\test\java\com\payhub\core\controls\base\EventControlTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.payhub.core.controls.base;

import static org.junit.jupiter.api.Assertions.*;

import com.payhub.core.enums.PaymentStatus;
import org.junit.jupiter.api.Test;

class EventControlTest {

    @Test
    void shouldExtendControlInjectorAndDeclareHandledEventType() {
        EventControl<String> control = new EventControl<>() {
            @Override
            public PaymentStatus getHandledEventType() {
                return PaymentStatus.COMPLETED;
            }

            @Override
            public Void execute(String input) {
                return null;
            }
        };

        assertEquals(PaymentStatus.COMPLETED, control.getHandledEventType());
        assertInstanceOf(ControlInjector.class, control);
        assertInstanceOf(Control.class, control);
        assertNull(control.execute("test"));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core-entities:test --tests "com.payhub.core.controls.base.EventControlTest"`
Expected: COMPILATION ERROR — `EventControl` class does not exist.

- [ ] **Step 3: Create `EventControl` abstract class**

```java
package com.payhub.core.controls.base;

import com.payhub.core.enums.PaymentStatus;

/**
 * A {@link ControlInjector} that reacts to a specific {@link PaymentStatus} domain event. Each
 * concrete subclass declares which event type it handles via {@link #getHandledEventType()}.
 *
 * @param <I> input type (typically {@code PaymentEvent})
 */
public abstract class EventControl<I> extends ControlInjector<I, Void> {

    public abstract PaymentStatus getHandledEventType();
}
```

- [ ] **Step 4: Create `EventListener` port interface**

```java
package com.payhub.core.infra;

import com.payhub.core.domain.PaymentEvent;

/**
 * Infrastructure interface for receiving domain events from a message broker. Implementations handle
 * deserialization and dispatch to {@code EventControl} instances.
 */
public interface EventListener {

    void onEvent(PaymentEvent event);
}
```

- [ ] **Step 5: Add `HttpClient` to `ControlInjector`**

In `ControlInjector.java`, add the import and field:
```java
// Add import:
import com.payhub.core.infra.HttpClient;

// In the class body, add after the existing fields:
@Getter
@Setter
public abstract class ControlInjector<I, O> implements Control<I, O> {

    protected AdapterClient adapterClient;
    protected DatabaseClient databaseClient;
    protected IdempotencyClient idempotencyClient;
    protected EventPublisher eventPublisher;
    protected SchedulerClient schedulerClient;
    protected ControlClient controlClient;
    protected HttpClient httpClient;  // NEW
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :core-entities:test --tests "com.payhub.core.controls.base.EventControlTest"`
Expected: PASS.

- [ ] **Step 7: Run full core-entities test suite**

Run: `./gradlew :core-entities:test`
Expected: PASS — all tests pass, no regressions.

- [ ] **Step 8: Commit**

```bash
git add core-entities/src/main/java/com/payhub/core/controls/base/EventControl.java core-entities/src/main/java/com/payhub/core/infra/EventListener.java core-entities/src/main/java/com/payhub/core/controls/base/ControlInjector.java core-entities/src/test/java/com/payhub/core/controls/base/EventControlTest.java
git commit -m "feat: add EventControl base class, EventListener port, and HttpClient to ControlInjector"
```

---

### Task 4: `ControlClient.getEventControls()` Port + Implementation

**Files:**
- Modify: `e:\payhub\core-entities\src\main\java\com\payhub\core\infra\ControlClient.java`
- Modify: `e:\payhub\infra-runtime\src\main\java\com\payhub\infra\runtime\ControlClientImpl.java`
- Create: `e:\payhub\infra-runtime\src\test\java\com\payhub\infra\runtime\ControlClientImplTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.payhub.infra.runtime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.payhub.core.controls.base.EventControl;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.infra.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

class ControlClientImplTest {

    private ApplicationContext applicationContext;
    private ControlClientImpl controlClient;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        applicationContext = mock(ApplicationContext.class);
        controlClient = new ControlClientImpl(
                mock(AdapterClient.class),
                mock(DatabaseClient.class),
                mock(IdempotencyClient.class),
                mock(EventPublisher.class),
                mock(SchedulerClient.class),
                mock(HttpClient.class),
                applicationContext);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldReturnEventControlsMatchingEventType() {
        EventControl completedControl = mock(EventControl.class);
        when(completedControl.getHandledEventType()).thenReturn(PaymentStatus.COMPLETED);

        EventControl initiatedControl = mock(EventControl.class);
        when(initiatedControl.getHandledEventType()).thenReturn(PaymentStatus.INITIATED);

        Map<String, EventControl> beans = Map.of(
                "c1", completedControl,
                "c2", initiatedControl);
        when(applicationContext.getBeansOfType(EventControl.class)).thenReturn((Map) beans);

        List<EventControl<PaymentEvent>> result = controlClient.getEventControls(PaymentStatus.COMPLETED);

        assertEquals(1, result.size());
        assertEquals(completedControl, result.get(0));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void shouldReturnEmptyListWhenNoMatch() {
        EventControl control = mock(EventControl.class);
        when(control.getHandledEventType()).thenReturn(PaymentStatus.COMPLETED);

        when(applicationContext.getBeansOfType(EventControl.class)).thenReturn((Map) Map.of("c1", control));

        List<EventControl<PaymentEvent>> result = controlClient.getEventControls(PaymentStatus.INITIATED);

        assertTrue(result.isEmpty());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :infra-runtime:test --tests "com.payhub.infra.runtime.ControlClientImplTest"`
Expected: COMPILATION ERROR — `getEventControls` doesn't exist yet, constructor missing `HttpClient` param.

- [ ] **Step 3: Add `getEventControls` to `ControlClient` port**

In `ControlClient.java`:
```java
package com.payhub.core.infra;

import com.payhub.core.controls.base.Control;
import com.payhub.core.controls.base.EventControl;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.enums.PaymentStatus;
import java.util.List;

public interface ControlClient {

    <T extends Control<?, ?>> T getControl(Class<T> controlType);

    List<EventControl<PaymentEvent>> getEventControls(PaymentStatus eventType);
}
```

- [ ] **Step 4: Update `ControlClientImpl` — add HttpClient to constructor, wire it in getControl, implement getEventControls**

```java
package com.payhub.infra.runtime;

import com.payhub.core.controls.base.Control;
import com.payhub.core.controls.base.ControlInjector;
import com.payhub.core.controls.base.EventControl;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.exception.ServiceNotFoundException;
import com.payhub.core.infra.*;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ControlClientImpl implements ControlClient {

    private final AdapterClient adapterClient;
    private final DatabaseClient databaseClient;
    private final IdempotencyClient idempotencyClient;
    private final EventPublisher eventPublisher;
    private final SchedulerClient schedulerClient;
    private final HttpClient httpClient;
    private final ApplicationContext applicationContext;

    public ControlClientImpl(
            AdapterClient adapterClient,
            DatabaseClient databaseClient,
            IdempotencyClient idempotencyClient,
            EventPublisher eventPublisher,
            SchedulerClient schedulerClient,
            HttpClient httpClient,
            ApplicationContext applicationContext) {
        this.adapterClient = adapterClient;
        this.databaseClient = databaseClient;
        this.idempotencyClient = idempotencyClient;
        this.eventPublisher = eventPublisher;
        this.schedulerClient = schedulerClient;
        this.httpClient = httpClient;
        this.applicationContext = applicationContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Control<?, ?>> T getControl(Class<T> controlType) {
        Map<String, T> beans = applicationContext.getBeansOfType(controlType);
        if (beans.isEmpty()) {
            throw new ServiceNotFoundException(controlType.getName());
        }
        if (beans.size() > 1) {
            log.warn("Multiple beans for " + controlType.getName() + " — using the first one");
        }
        T control = beans.values().iterator().next();

        if (control instanceof ControlInjector<?, ?> injector) {
            injector.setAdapterClient(adapterClient);
            injector.setDatabaseClient(databaseClient);
            injector.setIdempotencyClient(idempotencyClient);
            injector.setEventPublisher(eventPublisher);
            injector.setSchedulerClient(schedulerClient);
            injector.setControlClient(this);
            injector.setHttpClient(httpClient);
        }
        return control;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<EventControl<PaymentEvent>> getEventControls(PaymentStatus eventType) {
        Map<String, EventControl> allControls = applicationContext.getBeansOfType(EventControl.class);
        return allControls.values().stream()
                .filter(ec -> ec.getHandledEventType() == eventType)
                .peek(ec -> {
                    if (ec instanceof ControlInjector<?, ?> injector) {
                        injector.setAdapterClient(adapterClient);
                        injector.setDatabaseClient(databaseClient);
                        injector.setIdempotencyClient(idempotencyClient);
                        injector.setEventPublisher(eventPublisher);
                        injector.setSchedulerClient(schedulerClient);
                        injector.setControlClient(this);
                        injector.setHttpClient(httpClient);
                    }
                })
                .map(ec -> (EventControl<PaymentEvent>) ec)
                .toList();
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :infra-runtime:test --tests "com.payhub.infra.runtime.ControlClientImplTest"`
Expected: PASS — 2 tests pass.

- [ ] **Step 6: Commit**

```bash
git add core-entities/src/main/java/com/payhub/core/infra/ControlClient.java infra-runtime/src/main/java/com/payhub/infra/runtime/ControlClientImpl.java infra-runtime/src/test/java/com/payhub/infra/runtime/ControlClientImplTest.java
git commit -m "feat: add getEventControls to ControlClient and wire HttpClient in ControlClientImpl"
```

---

### Task 5: Update Templates — notifyUrl flow and event publishing

**Files:**
- Modify: `e:\payhub\core-applications\src\main\java\com\payhub\core\template\CreatePaymentControl.java`
- Modify: `e:\payhub\core-entities\src\main\java\com\payhub\core\controls\CreatePaymentTemplate.java`
- Modify: `e:\payhub\core-entities\src\main\java\com\payhub\core\controls\ProcessPaymentTemplate.java`
- Modify: `e:\payhub\core-entities\src\main\java\com\payhub\core\controls\CheckPaymentStatusTemplate.java`
- Create: `e:\payhub\core-entities\src\test\java\com\payhub\core\controls\CheckPaymentStatusTemplateTest.java`

- [ ] **Step 1: Write the failing test for CheckPaymentStatusTemplate event publishing**

```java
package com.payhub.core.controls;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.payhub.core.adapters.Adapter;
import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.domain.PaymentStatusResult;
import com.payhub.core.dto.CheckPaymentStatusRequest;
import com.payhub.core.enums.PaymentGateway;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.infra.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CheckPaymentStatusTemplateTest {

    private AdapterClient adapterClient;
    private DatabaseClient databaseClient;
    private EventPublisher eventPublisher;
    private TestCheckPaymentStatusTemplate template;

    @BeforeEach
    void setUp() {
        adapterClient = mock(AdapterClient.class);
        databaseClient = mock(DatabaseClient.class);
        eventPublisher = mock(EventPublisher.class);

        template = new TestCheckPaymentStatusTemplate();
        template.setAdapterClient(adapterClient);
        template.setDatabaseClient(databaseClient);
        template.setEventPublisher(eventPublisher);
        template.setIdempotencyClient(mock(IdempotencyClient.class));
        template.setSchedulerClient(mock(SchedulerClient.class));
        template.setControlClient(mock(ControlClient.class));
        template.setHttpClient(mock(HttpClient.class));
    }

    @Test
    void shouldPublishEventWhenPaymentCompletes() {
        Payment payment = new Payment();
        payment.setId("pay-1");
        payment.setOrderId("order-1");
        payment.setPaymentGateway(PaymentGateway.WECHAT_PAY);
        payment.setTransactionId("txn-123");
        payment.setNotifyUrl("https://partner.example.com/webhook");

        when(databaseClient.findByPaymentId("pay-1")).thenReturn(Optional.of(payment));

        Adapter adapter = mock(Adapter.class);
        when(adapterClient.getAdapter(PaymentGateway.WECHAT_PAY)).thenReturn(adapter);

        PaymentStatusResult result = new PaymentStatusResult(PaymentStatus.COMPLETED, "{\"status\":\"SUCCESS\"}");
        when(adapter.checkPaymentStatus("txn-123")).thenReturn(result);

        CheckPaymentStatusRequest request = new CheckPaymentStatusRequest();
        request.setPaymentId("pay-1");

        Boolean terminal = template.execute(request);

        assertTrue(terminal);

        ArgumentCaptor<PaymentEvent> captor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(eventPublisher).publish(captor.capture());
        PaymentEvent published = captor.getValue();
        assertEquals(PaymentStatus.COMPLETED, published.getType());
        assertEquals("pay-1", published.getPayment().getId());
        assertEquals("https://partner.example.com/webhook", published.getPayment().getNotifyUrl());
    }

    @Test
    void shouldPublishEventWhenPaymentFails() {
        Payment payment = new Payment();
        payment.setId("pay-2");
        payment.setPaymentGateway(PaymentGateway.ALIPAY);
        payment.setTransactionId("txn-456");

        when(databaseClient.findByPaymentId("pay-2")).thenReturn(Optional.of(payment));

        Adapter adapter = mock(Adapter.class);
        when(adapterClient.getAdapter(PaymentGateway.ALIPAY)).thenReturn(adapter);

        PaymentStatusResult result = new PaymentStatusResult(PaymentStatus.FAILED, "{\"status\":\"FAIL\"}");
        when(adapter.checkPaymentStatus("txn-456")).thenReturn(result);

        CheckPaymentStatusRequest request = new CheckPaymentStatusRequest();
        request.setPaymentId("pay-2");

        Boolean terminal = template.execute(request);

        assertTrue(terminal);

        ArgumentCaptor<PaymentEvent> captor = ArgumentCaptor.forClass(PaymentEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertEquals(PaymentStatus.FAILED, captor.getValue().getType());
    }

    @Test
    void shouldNotPublishEventWhenPaymentStillProcessing() {
        Payment payment = new Payment();
        payment.setId("pay-3");
        payment.setPaymentGateway(PaymentGateway.WECHAT_PAY);
        payment.setTransactionId("txn-789");

        when(databaseClient.findByPaymentId("pay-3")).thenReturn(Optional.of(payment));

        Adapter adapter = mock(Adapter.class);
        when(adapterClient.getAdapter(PaymentGateway.WECHAT_PAY)).thenReturn(adapter);

        PaymentStatusResult result = new PaymentStatusResult(PaymentStatus.PROCESSING, "{}");
        when(adapter.checkPaymentStatus("txn-789")).thenReturn(result);

        CheckPaymentStatusRequest request = new CheckPaymentStatusRequest();
        request.setPaymentId("pay-3");

        Boolean terminal = template.execute(request);

        assertFalse(terminal);
        verify(eventPublisher, never()).publish(any());
    }

    /** Minimal concrete subclass for testing the template. */
    static class TestCheckPaymentStatusTemplate extends CheckPaymentStatusTemplate {
        @Override
        protected void validate(CheckPaymentStatusRequest request) {
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core-entities:test --tests "com.payhub.core.controls.CheckPaymentStatusTemplateTest"`
Expected: FAIL — `eventPublisher.publish()` is never called, assertion fails (event not published on terminal status).

- [ ] **Step 3: Update `CheckPaymentStatusTemplate` to publish events on terminal status**

In `CheckPaymentStatusTemplate.java`, change the `execute` method to publish an event when terminal:
```java
@Override
public final Boolean execute(CheckPaymentStatusRequest request) {

    validate(request);

    Payment payment =
        databaseClient
            .findByPaymentId(request.getPaymentId())
            .orElseThrow(
                () -> new IllegalArgumentException("Payment not found: " + request.getPaymentId()));

    Adapter adapter = adapterClient.getAdapter(payment.getPaymentGateway());

    PaymentStatusResult result = adapter.checkPaymentStatus(payment.getTransactionId());

    payment.setStatus(result.getStatus());
    payment.setGatewayResponse(result.getRawResponse());
    databaseClient.save(payment);

    if (isTerminal(payment.getStatus())) {
        eventPublisher.publish(
            new PaymentEvent(
                payment.getStatus(),
                payment,
                System.currentTimeMillis()));
    }

    return isTerminal(payment.getStatus());
}
```

- [ ] **Step 4: Update `CreatePaymentTemplate` event construction**

In `CreatePaymentTemplate.java`, line 43-50, change the `new PaymentEvent(...)` call to use the new constructor:
```java
eventPublisher.publish(
    new PaymentEvent(
        PaymentStatus.INITIATED,
        payment,
        System.currentTimeMillis()));
```

- [ ] **Step 5: Update `ProcessPaymentTemplate` event construction**

In `ProcessPaymentTemplate.java`, line 66-73, change the `new PaymentEvent(...)` call:
```java
eventPublisher.publish(
    new PaymentEvent(
        PaymentStatus.PROCESSING,
        payment,
        System.currentTimeMillis()));
```

- [ ] **Step 6: Update `CreatePaymentControl` to pass notifyUrl from request to Payment**

In `CreatePaymentControl.java`, in `initiatePayment`, add:
```java
@Override
protected Payment initiatePayment(PaymentInitiateRequest request) {
    Payment payment = new Payment();
    payment.setId(UUID.randomUUID().toString());
    payment.setOrderId(request.getOrderId());
    payment.setAmount(request.getAmount());
    payment.setCurrency(request.getCurrency() != null ? request.getCurrency() : Currency.CNY);
    payment.setStatus(PaymentStatus.INITIATED);
    payment.setNotifyUrl(request.getNotifyUrl());
    return payment;
}
```

- [ ] **Step 7: Run test to verify it passes**

Run: `./gradlew :core-entities:test --tests "com.payhub.core.controls.CheckPaymentStatusTemplateTest"`
Expected: PASS — 3 tests pass.

- [ ] **Step 8: Run full test suite**

Run: `./gradlew :core-entities:test :core-applications:test`
Expected: PASS — all tests pass.

- [ ] **Step 9: Commit**

```bash
git add core-entities/src/main/java/com/payhub/core/controls/CheckPaymentStatusTemplate.java core-entities/src/main/java/com/payhub/core/controls/CreatePaymentTemplate.java core-entities/src/main/java/com/payhub/core/controls/ProcessPaymentTemplate.java core-applications/src/main/java/com/payhub/core/template/CreatePaymentControl.java core-entities/src/test/java/com/payhub/core/controls/CheckPaymentStatusTemplateTest.java
git commit -m "feat: add notifyUrl flow through templates and publish events on terminal status"
```

---

### Task 6: `EventPublisherImpl` — Rewrite with Kafka

**Files:**
- Rewrite: `e:\payhub\infra-message-client\src\main\java\com\payhub\infra\event\EventPublisherImpl.java`
- Create: `e:\payhub\infra-message-client\src\test\java\com\payhub\infra\event\EventPublisherImplTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.payhub.infra.event;

import static org.mockito.Mockito.*;

import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.infra.EncryptionClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

class EventPublisherImplTest {

    private KafkaTemplate<String, String> kafkaTemplate;
    private EncryptionClient encryptionClient;
    private EventPublisherImpl publisher;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        encryptionClient = mock(EncryptionClient.class);
        publisher = new EventPublisherImpl(kafkaTemplate, encryptionClient);
    }

    @Test
    void shouldEncryptThenSend() {
        Payment payment = new Payment();
        payment.setId("pay-1");
        payment.setOrderId("order-1");
        payment.setStatus(PaymentStatus.COMPLETED);
        PaymentEvent event = new PaymentEvent(PaymentStatus.COMPLETED, payment, 1717000000000L);

        when(encryptionClient.encrypt(anyString())).thenReturn("encrypted-payload");
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(null); // fire-and-forget, result ignored

        publisher.publish(event);

        verify(encryptionClient).encrypt(anyString());
        verify(kafkaTemplate).send("payment-events", "pay-1", "encrypted-payload");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :infra-message-client:test --tests "com.payhub.infra.event.EventPublisherImplTest"`
Expected: COMPILATION ERROR — `EventPublisherImpl` constructor doesn't match.

- [ ] **Step 3: Rewrite `EventPublisherImpl`**

```java
package com.payhub.infra.event;

import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.infra.EncryptionClient;
import com.payhub.core.infra.EventPublisher;
import com.payhub.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventPublisherImpl implements EventPublisher {

    private static final String TOPIC = "payment-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EncryptionClient encryptionClient;

    public EventPublisherImpl(KafkaTemplate<String, String> kafkaTemplate, EncryptionClient encryptionClient) {
        this.kafkaTemplate = kafkaTemplate;
        this.encryptionClient = encryptionClient;
    }

    @Override
    public void publish(PaymentEvent event) {
        String json = JsonUtils.toJson(event);
        String encrypted = encryptionClient.encrypt(json);
        String key = event.getPayment().getId();
        kafkaTemplate.send(TOPIC, key, encrypted);
        log.info("Event published: type={}, paymentId={}", event.getType(), event.getPayment().getId());
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :infra-message-client:test --tests "com.payhub.infra.event.EventPublisherImplTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add infra-message-client/src/main/java/com/payhub/infra/event/EventPublisherImpl.java infra-message-client/src/test/java/com/payhub/infra/event/EventPublisherImplTest.java
git commit -m "feat: rewrite EventPublisherImpl with Kafka + encryption"
```

---

### Task 7: `EventListenerImpl` — Kafka Consumer + Dispatch

**Files:**
- Create: `e:\payhub\infra-message-client\src\main\java\com\payhub\infra\event\EventListenerImpl.java`
- Create: `e:\payhub\infra-message-client\src\test\java\com\payhub\infra\event\EventListenerImplTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.payhub.infra.event;

import static org.mockito.Mockito.*;

import com.payhub.core.controls.base.EventControl;
import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.infra.ControlClient;
import com.payhub.core.infra.EncryptionClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventListenerImplTest {

    private EncryptionClient encryptionClient;
    private ControlClient controlClient;
    private EventListenerImpl listener;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        encryptionClient = mock(EncryptionClient.class);
        controlClient = mock(ControlClient.class);
        listener = new EventListenerImpl(encryptionClient, controlClient);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldDecryptDeserializeAndDispatch() {
        String encryptedPayload = "encrypted";
        String json = "{\"type\":\"COMPLETED\",\"payment\":{\"id\":\"pay-1\",\"orderId\":\"ord-1\",\"status\":\"COMPLETED\"},\"timestamp\":1717000000000}";

        when(encryptionClient.decrypt(encryptedPayload)).thenReturn(json);

        EventControl<PaymentEvent> mockControl = mock(EventControl.class);
        when(controlClient.getEventControls(PaymentStatus.COMPLETED))
                .thenReturn(List.of(mockControl));

        listener.onMessage(encryptedPayload);

        verify(encryptionClient).decrypt(encryptedPayload);
        verify(controlClient).getEventControls(PaymentStatus.COMPLETED);
        verify(mockControl).execute(any(PaymentEvent.class));
    }

    @Test
    void shouldHandleNoMatchingControls() {
        String encryptedPayload = "encrypted";
        String json = "{\"type\":\"INITIATED\",\"payment\":{\"id\":\"pay-2\",\"orderId\":\"ord-2\",\"status\":\"INITIATED\"},\"timestamp\":1717000000000}";

        when(encryptionClient.decrypt(encryptedPayload)).thenReturn(json);
        when(controlClient.getEventControls(PaymentStatus.INITIATED)).thenReturn(List.of());

        listener.onMessage(encryptedPayload);

        verify(encryptionClient).decrypt(encryptedPayload);
        verify(controlClient).getEventControls(PaymentStatus.INITIATED);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :infra-message-client:test --tests "com.payhub.infra.event.EventListenerImplTest"`
Expected: COMPILATION ERROR — `EventListenerImpl` does not exist.

- [ ] **Step 3: Create `EventListenerImpl`**

```java
package com.payhub.infra.event;

import com.payhub.core.controls.base.EventControl;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.infra.ControlClient;
import com.payhub.core.infra.EncryptionClient;
import com.payhub.core.infra.EventListener;
import com.payhub.core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventListenerImpl implements EventListener {

    private final EncryptionClient encryptionClient;
    private final ControlClient controlClient;

    public EventListenerImpl(EncryptionClient encryptionClient, ControlClient controlClient) {
        this.encryptionClient = encryptionClient;
        this.controlClient = controlClient;
    }

    @KafkaListener(topics = "payment-events")
    public void onMessage(String encryptedPayload) {
        String json = encryptionClient.decrypt(encryptedPayload);
        PaymentEvent event = JsonUtils.fromJson(json, PaymentEvent.class);
        log.info("Event received: type={}, paymentId={}", event.getType(), event.getPayment().getId());

        for (EventControl<PaymentEvent> control : controlClient.getEventControls(event.getType())) {
            control.execute(event);
        }
    }

    @Override
    public void onEvent(PaymentEvent event) {
        // Direct dispatch path (bypasses Kafka) — for in-process usage.
        onMessage(com.payhub.core.utils.JsonUtils.toJson(event));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :infra-message-client:test --tests "com.payhub.infra.event.EventListenerImplTest"`
Expected: PASS — 2 tests pass.

- [ ] **Step 5: Run full infra-message-client test suite**

Run: `./gradlew :infra-message-client:test`
Expected: PASS — all 3 test classes pass.

- [ ] **Step 6: Commit**

```bash
git add infra-message-client/src/main/java/com/payhub/infra/event/EventListenerImpl.java infra-message-client/src/test/java/com/payhub/infra/event/EventListenerImplTest.java
git commit -m "feat: add EventListenerImpl with Kafka consumer and EventControl dispatch"
```

---

### Task 8: Kafka Configuration

**Files:**
- Create: `e:\payhub\payment-platform\src\main\resources\kafka.yml`
- Create: `e:\payhub\payment-platform\src\main\java\com\payhub\platform\config\PayHubKafkaConfig.java`

- [ ] **Step 1: Create `kafka.yml`**

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

- [ ] **Step 2: Create `PayHubKafkaConfig`**

```java
package com.payhub.platform.config;

import com.payhub.infra.common.YamlPropertySourceFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "classpath:kafka.yml", factory = YamlPropertySourceFactory.class)
public class PayHubKafkaConfig {
}
```

- [ ] **Step 3: Verify project compiles**

Run: `./gradlew :payment-platform:compileJava`
Expected: PASS — compiles cleanly. Spring Boot auto-configures Kafka from `spring.kafka.*` properties loaded by `YamlPropertySourceFactory`.

- [ ] **Step 4: Commit**

```bash
git add payment-platform/src/main/resources/kafka.yml payment-platform/src/main/java/com/payhub/platform/config/PayHubKafkaConfig.java
git commit -m "feat: add Kafka configuration via kafka.yml and YamlPropertySourceFactory"
```

---

### Task 9: `NotifyPartnerControl`

**Files:**
- Create: `e:\payhub\core-applications\src\main\java\com\payhub\core\template\NotifyPartnerControl.java`
- Create: `e:\payhub\core-applications\src\test\java\com\payhub\core\template\NotifyPartnerControlTest.java`
- Modify: `e:\payhub\core-applications\src\main\resources\META-INF\services\com.payhub.core.controls.base.Control`

- [ ] **Step 1: Write the failing test**

```java
package com.payhub.core.template;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.infra.HttpClient;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotifyPartnerControlTest {

    private HttpClient httpClient;
    private NotifyPartnerControl control;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        control = new NotifyPartnerControl();
        control.setHttpClient(httpClient);
    }

    @Test
    void shouldHandleCompletedEvents() {
        assertEquals(PaymentStatus.COMPLETED, control.getHandledEventType());
    }

    @Test
    void shouldPostToNotifyUrlWhenCompleted() {
        Payment payment = new Payment();
        payment.setId("pay-1");
        payment.setOrderId("order-1");
        payment.setNotifyUrl("https://partner.example.com/webhook");
        payment.setStatus(PaymentStatus.COMPLETED);

        PaymentEvent event = new PaymentEvent(PaymentStatus.COMPLETED, payment, 1717000000000L);

        HttpClient.Response response = new HttpClient.Response(200, "OK");
        when(httpClient.post(eq("https://partner.example.com/webhook"), anyMap(), anyString()))
                .thenReturn(response);

        control.execute(event);

        verify(httpClient).post(
                eq("https://partner.example.com/webhook"),
                eq(Map.of("Content-Type", "application/json")),
                anyString());
    }

    @Test
    void shouldSkipWhenNotifyUrlIsNull() {
        Payment payment = new Payment();
        payment.setId("pay-2");
        payment.setNotifyUrl(null);

        PaymentEvent event = new PaymentEvent(PaymentStatus.COMPLETED, payment, 1717000000000L);

        control.execute(event);

        verify(httpClient, never()).post(anyString(), anyMap(), anyString());
    }

    @Test
    void shouldSkipWhenNotifyUrlIsBlank() {
        Payment payment = new Payment();
        payment.setId("pay-3");
        payment.setNotifyUrl("   ");

        PaymentEvent event = new PaymentEvent(PaymentStatus.COMPLETED, payment, 1717000000000L);

        control.execute(event);

        verify(httpClient, never()).post(anyString(), anyMap(), anyString());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core-applications:test --tests "com.payhub.core.template.NotifyPartnerControlTest"`
Expected: COMPILATION ERROR — `NotifyPartnerControl` does not exist.

- [ ] **Step 3: Create `NotifyPartnerControl`**

```java
package com.payhub.core.template;

import com.payhub.core.controls.base.EventControl;
import com.payhub.core.domain.Payment;
import com.payhub.core.domain.PaymentEvent;
import com.payhub.core.enums.PaymentStatus;
import com.payhub.core.utils.JsonUtils;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

- [ ] **Step 4: Update SPI services file**

Append to `e:\payhub\core-applications\src\main\resources\META-INF\services\com.payhub.core.controls.base.Control`:
```
com.payhub.core.template.NotifyPartnerControl
```

The full file should be:
```
com.payhub.core.template.CreatePaymentControl
com.payhub.core.template.ProcessPaymentControl
com.payhub.core.template.CheckPaymentStatusControl
com.payhub.core.template.NotifyPartnerControl
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :core-applications:test --tests "com.payhub.core.template.NotifyPartnerControlTest"`
Expected: PASS — 4 tests pass.

- [ ] **Step 6: Run full core-applications test suite**

Run: `./gradlew :core-applications:test`
Expected: PASS — all tests pass.

- [ ] **Step 7: Commit**

```bash
git add core-applications/src/main/java/com/payhub/core/template/NotifyPartnerControl.java core-applications/src/test/java/com/payhub/core/template/NotifyPartnerControlTest.java core-applications/src/main/resources/META-INF/services/com.payhub.core.controls.base.Control
git commit -m "feat: add NotifyPartnerControl for partner webhook delivery on payment completion"
```

---

### Task 10: Full Build Verification

- [ ] **Step 1: Build all modules**

Run: `./gradlew build`
Expected: PASS — all modules compile, all tests pass, Spotless check passes.

- [ ] **Step 2: Fix any Spotless violations if present**

Run: `./gradlew spotlessApply`
If any formatting issues exist, fix them and re-run step 1.

- [ ] **Step 3: Commit any formatting fixes**

```bash
git add . && git commit -m "style: apply Spotless formatting"
```
