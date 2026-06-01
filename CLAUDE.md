# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

```bash
# Build all modules (compiles + runs tests + Spotless formatting check)
./gradlew build

# Run a single service (bootRun)
./gradlew :payment-platform:bootRun

# Run tests for a single module
./gradlew :core-entities:test

# Run a single test class
./gradlew :core-entities:test --tests "com.payhub.core.controls.CreatePaymentTemplateTest"

# Fix formatting violations
./gradlew spotlessApply

# Check formatting (CI does this)
./gradlew spotlessCheck
```

Requires **JDK 21**. Gradle wrapper uses **9.4.0**. Infrastructure dependencies (Kafka, Redis, PostgreSQL, Jaeger, Prometheus, Grafana) are started via `docker compose up -d`.

Key docker-compose services and ports:

| Service | Port | Purpose |
|---|---|---|
| Kafka | 9092 | Message broker |
| Redis | 6379 | Cache / distributed lock |
| PostgreSQL | 5432 | Payment & Quartz persistence |
| Kafdrop | 9000 | Kafka UI (browse topics/consumers) |
| Jaeger | 16686 | Distributed tracing UI |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Metrics dashboards (admin/admin) |

Environment variables / profile configuration:

The profile is set in `application.yml` as `${SPRING_PROFILES_ACTIVE:local}` — defaults to `local` for development, overridden via the `SPRING_PROFILES_ACTIVE` env var in other environments (dev/int/uat/prod). Environment-specific overrides live in `application-{profile}.yml`.

| Property | Default (in application-local.yml) | Referenced by |
|---|---|---|
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka auto-configuration |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/payhub` | Quartz / datasource auto-config |
| `spring.datasource.username` | `payhub` | Quartz / datasource auto-config |
| `spring.datasource.password` | `payhub123` | Quartz / datasource auto-config |

## Architecture

This is a **hexagonal (ports-and-adapters)** payment processing system with **SPI-driven plugin discovery**.

### Module Dependency Rules

```
                    ┌─────────────────────┐
                    │   core-entities      │  ← ZERO framework deps (pure Java + Lombok + SLF4J)
                    │  domain, ports, SPI  │
                    └──────────┬──────────┘
                               │ depends on
              ┌────────────────┼────────────────┐
              ▼                ▼                 ▼
    ┌─────────────┐  ┌──────────────┐  ┌─────────────────┐
    │core-applications│ │wechatpay/alipay│ │infra-* modules  │
    │(Control impls)  │ │(Adapter impls) │ │(port impls)     │
    └─────────────┘  └──────────────┘  └─────────────────┘
                                               │
                                               ▼
                                     ┌──────────────────┐
                                     │ payment-platform  │  ← Composition root
                                     │ (Spring Boot app) │
                                     └──────────────────┘
```

**Rule**: The four "clean" modules (`core-entities`, `core-applications`, `wechatpay-adapter`, `alipay-adapter`) must NOT import Spring or any framework. Only `infra-*` and `payment-platform` get Spring Boot dependencies.

A fifth module, `infra-common`, contains shared Spring utilities used by other infra modules — most notably `YamlPropertySourceFactory`, which loads `.yml` files as `PropertySource` instances (used by `QuartzConfig` for `scheduler-defaults.yml` and `PayHubKafkaConfig` for `kafka.yml`).

### Two Parallel SPI Mechanisms

**Adapter SPI** (`com.payhub.core.adapters.Adapter`): Payment gateway implementations (WeChat Pay, Alipay). Discovered via `ServiceLoader.load(Adapter.class)`, registered as **singleton** Spring beans by `AdapterBeanDefinitionRegistrar`. Each declares itself in `META-INF/services/com.payhub.core.adapters.Adapter`.

**Control SPI** (`com.payhub.core.controls.base.Control<I,O>`): Business logic templates. Discovered via `ServiceLoader.load(Control.class)`, registered as **prototype** Spring beans by `ControlBeanDefinitionRegistrar`. Each declares itself in `META-INF/services/com.payhub.core.controls.base.Control`.

**Spring Boot 3.x auto-configuration**: Each `infra-*` module registers its `@Component` beans via `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (the Spring Boot 3.x replacement for `spring.factories`). `infra-runtime` registers the two `BeanDefinitionRegistrar`s plus `ControlClientImpl` and `AdapterClientImpl`. Other infra modules register their own port implementations. No explicit `@ComponentScan` is needed — `@SpringBootApplication` on `payment-platform` picks everything up through auto-configuration.

### Dependency Injection Pattern

Controls and adapters do NOT use `@Autowired` or constructor injection. Instead:

1. `ControlInjector<I,O>` — abstract base for controls, declares `@Setter` fields for all 7 infra ports (`adapterClient`, `databaseClient`, `idempotencyClient`, `eventPublisher`, `schedulerClient`, `controlClient`, `httpClient`).
2. `AdapterInjector` — abstract base for adapters, declares `@Setter` for `httpClient`.
3. `ControlClientImpl.getControl()` / `AdapterClientImpl.getAdapter()` call the setters **at lookup time** after the prototype bean is created.

This keeps `core-entities` and `core-applications` free of DI framework imports.

### Key Port Interfaces (in `core-entities/src/.../com/payhub/core/infra/`)

| Port | Purpose | Default Impl |
|---|---|---|
| `HttpClient` | HTTP calls to gateways & partner webhooks | `infra-http-client` (OkHttp) |
| `DatabaseClient` | Payment persistence | `infra-database-client` (in-memory `ConcurrentHashMap`) |
| `IdempotencyClient` | Dedup + distributed locks | `infra-idempotent-client` (in-memory `ConcurrentHashMap` + `ReentrantLock`) |
| `EventPublisher` | Domain event publishing to Kafka | `infra-message-client` (`KafkaTemplate`) |
| `SchedulerClient` | Recurring/scheduled jobs | `infra-scheduler` (Quartz) |
| `AdapterClient` | Gateway adapter lookup | `infra-runtime` |
| `ControlClient` | Control resolution + wiring | `infra-runtime` |
| `EncryptionClient` | Message encryption/decryption | `infra-encryption-client` (no-op stub) |
| `CacheClient` | Cache with TTL | (no impl yet) |
| `SecretsClient` | API keys/signing secrets | (no impl yet) |

### Control Hierarchy (Template Method)

```
Control<I, O>                          (SPI root: O execute(I))
  └── ControlInjector<I, O>            (holds all 7 @Setter infra ports)
        ├── CreatePaymentTemplate       (lock → persist → publish event)
        │     └── CreatePaymentControl  (concrete, in core-applications)
        ├── ProcessPaymentTemplate      (resolve adapter → call gateway → schedule poll)
        │     └── ProcessPaymentControl (concrete, in core-applications)
        ├── CheckPaymentStatusTemplate  (poll gateway → self-cancel when terminal)
        │     └── CheckPaymentStatusControl (concrete, in core-applications)
        └── EventControl<I extends BaseEvent>  (adds getHandledEventType(); return type Void)
              └── NotifyPartnerTemplate  (POSTs to partner webhook on terminal events)
                    └── NotifyPartnerControl (concrete, in core-applications)
```

Templates in `core-entities` handle locking, idempotency, persistence, and event publishing. Concrete subclasses in `core-applications` only fill in validation, construction, and response building.

### Payment Flow

The REST layer lives in `payment-platform`:

- `PaymentController` (`/api/payments`) exposes the two endpoints.
- `PaymentService` is a thin `@Service` that resolves the right `Control` via `controlClient.getControl(ControlClass.class)` and calls `execute()`. It exists so the controller doesn't depend on the SPI directly.

1. `POST /api/payments/initiate` → `PaymentController` → `PaymentService` → `CreatePaymentControl` → creates payment, persists, publishes event
2. `POST /api/payments/process` → `PaymentController` → `PaymentService` → `ProcessPaymentControl` → resolves gateway adapter by name, calls `adapter.processPayment()`, schedules recurring status poll via Quartz
3. `CheckPaymentStatusControl` runs every 30s (for up to 5min) via `SchedulerClientImpl` → `adapter.checkPaymentStatus()` → self-cancels when terminal (COMPLETED/FAILED), publishes event. Polling interval and max duration are configured in `core-applications` via `payment-config.yml`, loaded through `YamlUtils`.

### Event-Driven Architecture

Domain events flow through the system asynchronously via Kafka:

```
Template publishes PaymentEvent
        │
        ▼
EventPublisherImpl.publish(event)
  (JsonUtils.toJson → ProducerRecord with "eventType" header = event.getClass().getName())
  [EncryptingValueSerializer encrypts value at the Kafka producer factory level]
  [After-commit: only sends if current TX commits; skips on rollback]
        │
        ▼
Kafka topic "payment-events" (keyed by payment ID for per-payment ordering)
        │
        ▼
EventListenerImpl.onMessage(@KafkaListener)
  [EncryptingValueDeserializer decrypts value at the Kafka consumer factory level]
  (reads "eventType" header → Class.forName() + cache → JsonUtils.fromJson)
        │
        ▼
controlClient.getEventControls() — returns all EventControl beans
        │
        ▼
Dispatch: for each EventControl, if control.getHandledEventType().isAssignableFrom(event.getClass())
        │
        ▼
NotifyPartnerControl.execute()
  (extracts payment.notifyUrl, POSTs JSON body via HttpClient to partner webhook)
```

**Key points:**

- **`BaseEvent`** (`com.payhub.core.event.BaseEvent`) is a marker interface for domain events with an optional `key()` method that provides the Kafka record key for partitioning.
- **Event type resolution**: The `EventPublisherImpl` sets a Kafka header `"eventType"` = the event's fully-qualified class name (e.g. `com.payhub.core.domain.PaymentEvent`). The `EventListenerImpl` reads this header, resolves it to a `Class` via `Class.forName()` (cached in a `ConcurrentHashMap`), then deserializes the JSON body with `JsonUtils.fromJson(json, resolvedClass)`. There is **no** Jackson `@JsonTypeInfo`/`@JsonSubTypes` polymorphism — type discrimination is purely header-based.
- **After-commit publishing**: `EventPublisherImpl.publish()` registers a `TransactionSynchronization` — the event is only sent to Kafka after the current transaction commits. If the transaction rolls back, the event is silently skipped.
- **`EventControl<I extends BaseEvent>`** extends `ControlInjector<I, Void>` and adds `getHandledEventType()` which returns the `Class` of events it handles. Dispatch uses `Class.isAssignableFrom()`, so subclasses of the declared event type also match. The return type is `Void` — event handlers produce no output.
- **`EventListenerImpl`** is a standalone `@Component` with `@KafkaListener` — it does NOT implement a port interface. The old `EventListener` port interface was deleted; the consumer side is purely infrastructure.
- **`NotifyPartnerControl`** sends a webhook POST to the partner's `notifyUrl` (from the payment aggregate) when a payment reaches a terminal status. If `notifyUrl` is null/blank, it silently skips.
- **Encryption** is handled at the Kafka serialization layer via `EncryptingValueSerializer`/`EncryptingValueDeserializer` (configured in `kafka.yml`). These use `SpringContextHolder` to lazily look up the `EncryptionClient` bean, since Kafka serializer/deserializer instances are created before the Spring context is fully ready.
- **`JsonUtils`** (`com.payhub.core.utils.JsonUtils`) is the centralized Jackson wrapper, used by both the event publisher (serialization) and listener (deserialization).

### Kafka Configuration & Serde

Kafka settings live in `kafka.yml` (`infra-message-client/src/main/resources/kafka.yml`), loaded via `YamlPropertySourceFactory` into `PayHubKafkaConfig`. It configures:

- **`bootstrap-servers`**: Set directly in `application-{profile}.yml` as `spring.kafka.bootstrap-servers`. Not defined in `kafka.yml` — it varies by environment.
- **Consumer group-id**: Set in the `@KafkaListener` annotation (`groupId = "payhub-consumer"`) on `EventListenerImpl`, not in `kafka.yml`.
- **`EncryptingValueSerializer`** / **`EncryptingValueDeserializer`**: Custom Kafka serde that encrypts/decrypts message values. Both use `SpringContextHolder.getBean(EncryptionClient.class)` to lazily obtain the `EncryptionClient` bean, because Kafka creates serializer/deserializer instances before the Spring context is fully available.
- **Retry with `ExponentialBackOff`**: In `payhub.kafka.consumer.retry.*`. Non-retryable exceptions (`MissingHeaderException`, `UnknownEventTypeException`) skip retry and are logged. All other failures retry with exponential backoff.
- **`SpringContextHolder`** (`com.payhub.infra.message.SpringContextHolder`) implements `ApplicationContextAware` to expose the Spring context statically — needed only by the serde classes.

### Scheduler

`infra-scheduler` uses Quartz with `ControlJob` (a `QuartzJobBean`) as the universal job class. Control metadata (type, request JSON, max duration) is serialized into the `JobDataMap`. `QuartzConfig` loads `scheduler-defaults.yml` and uses `AutowiringSpringBeanJobFactory` so that `ControlJob` instances get Spring DI.

Datasource credentials (`url`, `username`, `password`) are set directly in `application-{profile}.yml` — they vary by environment. Only the driver class and Quartz-specific settings live in `scheduler-defaults.yml`.

Uses JDBC job store with PostgreSQL, clustered mode enabled, 4 thread pool.

### Test Patterns

- **JUnit 5 + Mockito** across all modules
- Test classes are **package-private** (no `public` modifier)
- Dependencies are wired **manually via setters** — no Spring test context. Mock the infra ports and call setters on the concrete control before `execute()`.
- Method naming: `shouldXxxWhenYyy` (e.g. `shouldPostToNotifyUrlWhenCompleted`, `shouldSkipWhenNotifyUrlIsNull`)
- Abstract templates are tested by creating **inline anonymous subclasses** that implement the abstract methods (e.g. `new CheckPaymentStatusTemplate() { @Override void validate(...) {} }`)
- `JsonUtils` is used for serialization round-trip tests of domain objects

### Exception Hierarchy

All domain exceptions live in `core-entities/src/.../com/payhub/core/exception/`. The hierarchy is:

```
RuntimeException
  ├── PaymentProcessingException          (base for payment-related failures)
  │     ├── LockAcquisitionException      → 409 Conflict
  │     ├── DuplicatePaymentException     → 409 Conflict
  │     ├── ServiceNotFoundException      → 404 Not Found
  │     ├── PartnerNotificationException  → 502 Bad Gateway
  │     └── ControlInstantiationException → 500
  ├── SerializationException              → 500
  └── JobSchedulingException              → 500
```

Bare `RuntimeException` is never thrown — always use one of the above.

### Global Exception Handler

`GlobalExceptionHandler` (`payment-platform/.../handler/`) is a `@RestControllerAdvice` that maps exceptions to HTTP statuses via `ProblemDetail` (RFC 9457). It logs at `warn` for client/recoverable errors (409, 404) and `error` for server/infrastructure failures (500, 502).

## CI

GitHub Actions in `.github/workflows/payhub-ci.yml`: runs `spotlessCheck` then `./gradlew build` on every push/PR to `main`. Uses JDK 21 Temurin and the Gradle setup action.
