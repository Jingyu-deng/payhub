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

Requires **JDK 21**. Infrastructure dependencies (Kafka, Redis, PostgreSQL) are started via `docker compose up -d`.

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

**Rule**: The four "clean" modules (`core-entities`, `core-applications`, `wechatpay-adapter`, `alipay-adapter`) must NOT import Spring or any framework. Only `infra-*` and `payment-platform` get Spring Boot dependencies (see `build.gradle` line 35).

### Two Parallel SPI Mechanisms

**Adapter SPI** (`com.payhub.core.adapters.Adapter`): Payment gateway implementations (WeChat Pay, Alipay). Discovered via `ServiceLoader.load(Adapter.class)`, registered as **singleton** Spring beans by `AdapterBeanDefinitionRegistrar`. Each declares itself in `META-INF/services/com.payhub.core.adapters.Adapter`.

**Control SPI** (`com.payhub.core.controls.base.Control<I,O>`): Business logic templates. Discovered via `ServiceLoader.load(Control.class)`, registered as **prototype** Spring beans by `ControlBeanDefinitionRegistrar`. Each declares itself in `META-INF/services/com.payhub.core.controls.base.Control`.

### Dependency Injection Pattern

Controls and adapters do NOT use `@Autowired` or constructor injection. Instead:

1. `ControlInjector<I,O>` — abstract base for controls, declares `@Setter` fields for all 6 infra ports (`adapterClient`, `databaseClient`, `idempotencyClient`, `eventPublisher`, `schedulerClient`, `controlClient`).
2. `AdapterInjector` — abstract base for adapters, declares `@Setter` for `httpClient`.
3. `ControlClientImpl.getControl()` / `AdapterClientImpl.getAdapter()` call the setters **at lookup time** after the prototype bean is created.

This keeps `core-entities` and `core-applications` free of DI framework imports.

### Key Port Interfaces (in `core-entities/src/.../com/payhub/core/infra/`)

| Port | Purpose | Default Impl |
|---|---|---|
| `HttpClient` | HTTP calls to gateways | `infra-http-client` (OkHttp) |
| `DatabaseClient` | Payment persistence | `infra-database-client` (in-memory `ConcurrentHashMap`) |
| `IdempotencyClient` | Dedup + distributed locks | `infra-idempotent-client` (in-memory `ConcurrentHashMap` + `ReentrantLock`) |
| `EventPublisher` | Domain event publishing | `infra-message-client` (SLF4J logger only — Kafka stub) |
| `SchedulerClient` | Recurring/scheduled jobs | `infra-scheduler` (Quartz) |
| `AdapterClient` | Gateway adapter lookup | `infra-runtime` |
| `ControlClient` | Control resolution + wiring | `infra-runtime` |
| `CacheClient` | Cache with TTL | (no impl yet) |
| `SecretsClient` | API keys/signing secrets | (no impl yet) |

### Payment Flow

1. `POST /api/payments/initiate` → `CreatePaymentControl` → creates payment, persists, publishes `INITIATED` event
2. `POST /api/payments/process` → `ProcessPaymentControl` → resolves gateway adapter by name, calls `adapter.processPayment()`, schedules recurring status poll via Quartz
3. `CheckPaymentStatusControl` runs every 30s (for up to 5min) via `SchedulerClientImpl` → `adapter.checkPaymentStatus()` → self-cancels when terminal (COMPLETED/FAILED)

All three controls extend the Template Method classes from `core-entities` (`CreatePaymentTemplate`, `ProcessPaymentTemplate`, `CheckPaymentStatusTemplate`) which handle locking, idempotency, persistence, and event publishing — concrete subclasses in `core-applications` only fill in validation, construction, and response building.

### Scheduler

`infra-scheduler` uses Quartz with `ControlJob` (a `QuartzJobBean`) as the universal job class. Control metadata (type, request JSON, max duration) is serialized into the `JobDataMap`. `QuartzConfig` loads `scheduler-defaults.yml` and uses `AutowiringSpringBeanJobFactory` so that `ControlJob` instances get Spring DI.

## CI

GitHub Actions in `.github/workflows/payhub-ci.yml`: runs `spotlessCheck` then `./gradlew build` on every push/PR to `main`.
