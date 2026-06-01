# PayHub — Unified Payment Processing Platform

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.4.0-black.svg)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-24.0-blue.svg)](https://www.docker.com/)

## Overview

PayHub is a production-grade payment processing platform built on a **hexagonal (ports-and-adapters)** architecture. It demonstrates:

- **SPI-based pluggable payment gateways** (WeChat Pay, Alipay) with ServiceLoader discovery
- **Event-driven architecture** with Apache Kafka — at-least-once delivery with after-commit publishing
- **Template Method pattern** for payment flows — templates in `core-entities`, concrete controls in `core-applications`
- **Framework-free domain core** — zero Spring/DI imports in domain logic
- **Full containerisation** with Docker Compose
- **Production-ready observability** — Micrometer tracing, Prometheus metrics, Grafana dashboards, Jaeger

## Tech Stack

| Component          | Technology                                   |
|--------------------|----------------------------------------------|
| Language           | Java 21                                      |
| Framework          | Spring Boot 3.3.4                            |
| Message Broker     | Apache Kafka (with Kafdrop UI)               |
| Cache / Lock       | Redis (Redisson for distributed locks)       |
| Database           | PostgreSQL 15                                |
| Build Tool         | Gradle 9.4.0 (multi-module)                  |
| Containerisation   | Docker, Docker Compose                       |
| Observability      | Micrometer, Prometheus, Grafana, Jaeger      |
| Tracing            | Micrometer Tracing (OTel + OTLP)             |
| Testing            | JUnit 5, Mockito                             |

## Architecture

This is a **hexagonal architecture** with ports in `core-entities` and adapters in `infra-*` modules. No framework imports in the domain core.

### Modules

| Module | Layer | Description |
|--------|-------|-------------|
| `core-entities` | Domain | Payment aggregate, port interfaces, enums, DTOs, SPI contracts, template base classes |
| `core-applications` | Application | Concrete Control implementations (template method subclasses) |
| `wechatpay-adapter` | Adapter | WeChat Pay gateway integration |
| `alipay-adapter` | Adapter | Alipay gateway integration |
| `infra-http-client` | Infrastructure | OkHttp-based `HttpClient` |
| `infra-database-client` | Infrastructure | In-memory `DatabaseClient` |
| `infra-idempotent-client` | Infrastructure | In-memory `IdempotencyClient` |
| `infra-message-client` | Infrastructure | Kafka `EventPublisher` + `EventListener` |
| `infra-encryption-client` | Infrastructure | No-op `EncryptionClient` stub |
| `infra-scheduler` | Infrastructure | Quartz-based `SchedulerClient` |
| `infra-common` | Infrastructure | Shared Spring utilities (`YamlPropertySourceFactory`) |
| `infra-runtime` | Infrastructure | SPI wiring — `ControlClient`, `AdapterClient`, bean registrars |
| `payment-platform` | Composition Root | Spring Boot app — controllers, service layer, configuration |

### Payment Flow

1. **`POST /api/payments/initiate`** — creates a payment, persists it, publishes a domain event
2. **`POST /api/payments/process`** — resolves the payment gateway adapter, calls the gateway, schedules recurring status polling
3. **Status polling** — a Quartz job polls the gateway every 30s (up to 5 min); self-cancels when terminal (COMPLETED/FAILED), publishes an event
4. **Partner notification** — the Kafka listener dispatches terminal events to `NotifyPartnerControl`, which POSTs to the partner's `notifyUrl`

### Event Bus

Domain events are published to the Kafka topic `payment-events` (keyed by payment ID). Event type is communicated via a Kafka header (`eventType` → fully-qualified class name). The consumer resolves the class via `Class.forName()`, deserializes with Jackson, and dispatches to matching `EventControl` beans.

For detailed architecture documentation, see [CLAUDE.md](CLAUDE.md).

## Quick Start

### Prerequisites

- Docker Desktop (or Docker + Compose)
- Java 21 (JDK)
- Git

### 1. Clone the repository

    git clone https://github.com/yourusername/payhub.git
    cd payhub

### 2. Start all dependencies

    docker compose up -d

This starts:

| Service | Port | Purpose |
|---------|------|---------|
| Kafka | 9092 | Message broker |
| Redis | 6379 | Cache / distributed lock |
| PostgreSQL | 5432 | Payment & Quartz persistence |
| Kafdrop | 9000 | Kafka UI (browse topics/consumers) |
| Prometheus | 9090 | Metrics collection |
| Grafana | 3000 | Metrics dashboards (admin/admin) |
| Jaeger | 16686 | Distributed tracing UI |

### 3. Run the application

    ./gradlew :payment-platform:bootRun

### 4. Send a test request

Initiate a payment:

    curl -X POST http://localhost:8080/api/payments/initiate \
      -H "Content-Type: application/json" \
      -d '{"orderId":"ORD-001","amount":99.90,"currency":"CNY","notifyUrl":"https://partner.example.com/callback"}'

Process the payment:

    curl -X POST http://localhost:8080/api/payments/process \
      -H "Content-Type: application/json" \
      -d '{"paymentId":"<id-from-initiate>","orderId":"ORD-001","gatewayName":"WECHAT_PAY","params":{}}'

### 5. Verify

- **Kafdrop UI** — browse the `payment-events` topic at http://localhost:9000
- **Jaeger** — view distributed traces at http://localhost:16686
- **Prometheus** — query metrics at http://localhost:9090 (e.g. `http_server_requests_seconds_count`)
- **Grafana** — dashboards at http://localhost:3000 (admin/admin)

### Build & Test

```bash
# Build all modules (compile + test + formatting check)
./gradlew build

# Run tests for a single module
./gradlew :core-entities:test

# Run a single test class
./gradlew :core-entities:test --tests "com.payhub.core.controls.CreatePaymentTemplateTest"

# Fix formatting violations (Spotless)
./gradlew spotlessApply
```

Requires **JDK 21**.

## Payment Gateway SPI

PayHub uses **Java SPI (ServiceLoader)** to dynamically discover payment gateway implementations:

- **WeChat Pay** — `wechatpay-adapter` module (`WechatPayAdapter`)
- **Alipay** — `alipay-adapter` module (`AlipayAdapter`)

Adding a new gateway requires implementing `com.payhub.core.adapters.Adapter` and registering it in `META-INF/services/com.payhub.core.adapters.Adapter`.

## Architecture Decisions (ADRs)

- [ADR-001](docs/adr/001-message-queue.md) — Message queue selection (Kafka vs RabbitMQ vs RocketMQ)
- [ADR-002](docs/adr/002-distributed-lock.md) — Distributed lock strategy (Redisson vs RedisTemplate)
- [ADR-003](docs/adr/003-payment-gateway-abstraction.md) — SPI for payment gateway pluggability

## Project Status

| Feature | Status |
|---------|--------|
| Docker Compose (Kafka, Redis, PostgreSQL) | ✅ Done |
| Payment REST API | ✅ Done |
| Kafka producer / consumer | ✅ Done |
| In-memory persistence | ✅ Done |
| Redis idempotency (duplicate prevention) | ✅ Done |
| Distributed locks (Redisson) | ✅ Done |
| Kafka reliability (acks=all, idempotence) | ✅ Done |
| Dead-letter queue (DLQ) | ✅ Done |
| SPI payment gateways (WeChat, Alipay) | ✅ Done |
| Unit / integration tests | ✅ Done |
| Prometheus + Grafana | ✅ Done |
| Distributed tracing (OTel + Jaeger) | ✅ Done |
| PostgreSQL persistence (Quartz JDBC store) | ✅ Done |
| Partner webhook notification | ✅ Done |
| Kubernetes deployment (Minikube) | ⏳ Planned |
| Load testing (JMeter) | ⏳ Planned |

## Contributing

This project is for learning and portfolio purposes.
