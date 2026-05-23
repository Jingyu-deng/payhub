# PayHub – Unified Payment Gateway & Order Processing Platform

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.4.0-black.svg)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-24.0-blue.svg)](https://www.docker.com/)

## Overview

PayHub is a production‑grade microservices system that processes orders and payments asynchronously. It demonstrates:

- **Event‑driven architecture** with Apache Kafka
- **Reliable messaging** (acks=all, idempotency, dead‑letter queue)
- **Distributed locking** and idempotency with Redis
- **SPI‑based pluggable payment gateways** (WeChat Pay, Alipay)
- **Persistent storage** using PostgreSQL and JPA
- **Full containerisation** with Docker Compose
- **Production‑ready configuration** for real‑world deployment

This project is designed as a portfolio piece for senior/architect roles, showcasing both code quality and architectural documentation (C4 diagrams, ADRs).

## Tech Stack

| Component          | Technology                                   |
|--------------------|----------------------------------------------|
| Language           | Java 21                                      |
| Framework          | Spring Boot 3.3.4, Spring Data JPA          |
| Message Broker     | Apache Kafka (with Kafdrop UI)              |
| Cache / Lock       | Redis (Redisson for distributed locks)      |
| Database           | PostgreSQL 15                                |
| Build Tool         | Gradle (multi‑module)                        |
| Containerisation   | Docker, Docker Compose                       |
| Observability      | Micrometer, Prometheus, Grafana, Jaeger     |
| Tracing            | Micrometer Tracing (OTel + OTLP)        |
| Testing            | JUnit 5, Mockito, Testcontainers            |

## Architecture

![C4 Container Diagram](docs/c4-container-diagram.png)

The system consists of:
- **Order Service** – REST API to create orders, persists order, publishes `OrderCreatedEvent` to Kafka.
- **Payment Service** – consumes `OrderCreatedEvent`, uses SPI‑discovered payment gateways, records payment in DB.
- **Kafka** – asynchronous event bus.
- **Redis** – idempotency store and distributed lock manager.
- **PostgreSQL** – stores orders, payments, audit logs.

## Payment Gateway SPI

PayHub uses **Java SPI (Service Provider Interface)** to dynamically discover payment gateway implementations. Currently supported:

- **WeChat Pay** – `wechat-pay-adapter` module
- **Alipay** – `alipay-adapter` module

Adding a new gateway requires a new module implementing `PaymentGateway` and providing a `META-INF/services/com.payhub.common.payment.PaymentGateway` file.

## Quick Start

### Prerequisites

- Docker Desktop (or Docker + Compose)
- Java 21 (JDK)
- Git

### 1. Clone the repository

    git clone https://github.com/yourusername/payhub.git
    cd payhub

### 2. Start all dependencies

    docker-compose up -d

This starts:

- **Kafka** (port 9092) + Zookeeper (port 2181)
- **Redis** (port 6379)
- **PostgreSQL** (port 5432)
- **Kafdrop** (port 9000) – Kafka UI
- **Prometheus** (port 9090) – metrics collection
- **Grafana** (port 3000) – metrics dashboards (admin/admin)
- **Jaeger** (port 16686) – distributed tracing

### 3. Run the Spring Boot services

You can run them from your IDE (run the main classes) or via Gradle:

    # Terminal 1 – Order Service
    ./gradlew :order-service:bootRun

    # Terminal 2 – Payment Service
    ./gradlew :payment-service:bootRun

### 4. Send a test order

    curl -X POST http://localhost:8080/api/orders \
      -H "Content-Type: application/json" \
      -d '{"productId":"P001","quantity":2,"userId":"user123"}'

### 5. Verify

- **Order Service logs** – should show order saved and event sent.
- **Payment Service logs** – should show event received and payment recorded.
- **Database**:

    docker exec -it payhub-postgres psql -U payhub -d payhub -c "SELECT * FROM orders;"
    docker exec -it payhub-postgres psql -U payhub -d payhub -c "SELECT * FROM payments;"

- **Kafdrop UI** – open http://localhost:9000, browse `order-events` topic.

### 6. Observability

- **Prometheus** – open http://localhost:9090, query `http_server_requests_seconds_count`.
- **Grafana** – open http://localhost:3000 (admin/admin), browse the pre-provisioned "PayHub - Service Overview" dashboard.
- **Jaeger** – open http://localhost:16686, select a service and click "Find Traces".
- **Actuator metrics**:
  - http://localhost:8080/actuator/metrics (Order Service)
  - http://localhost:8081/actuator/metrics (Payment Service)
  - http://localhost:8080/actuator/prometheus (Order Service Prometheus endpoint)
  - http://localhost:8081/actuator/prometheus (Payment Service Prometheus endpoint)

## Project Status

| Feature | Status |
|---------|--------|
| Docker Compose (Kafka, Redis, PostgreSQL) | ✅ Done |
| Order Service REST API | ✅ Done |
| Kafka producer / consumer | ✅ Done |
| JPA + PostgreSQL persistence | ✅ Done |
| Manual offset commit | ✅ Done |
| Redis idempotency (duplicate prevention) | ✅ Done |
| Distributed locks (Redisson) | ✅ Done |
| Kafka reliability (acks=all, idempotence) | ✅ Done |
| Dead‑letter queue (DLQ) | ✅ Done |
| SPI payment gateways (WeChat, Alipay) | ✅ Done |
| Unit / integration tests | ⏳ Planned |
| Prometheus + Grafana | ✅ Done |
| Distributed tracing (OTel + Jaeger) | ✅ Done |
| Kubernetes deployment (Minikube) | ⏳ Planned |
| Load testing (JMeter) | ⏳ Planned |

## Architecture Decisions (ADRs)

- [ADR‑001](docs/adr/001-message-queue.md) – Message queue selection (Kafka vs RabbitMQ vs RocketMQ)
- [ADR‑002](docs/adr/002-distributed-lock.md) – Distributed lock strategy (Redisson vs RedisTemplate)
- [ADR‑003](docs/adr/003-payment-gateway-abstraction.md) – SPI for payment gateway pluggability

## Project Structure

    payhub/
    ├── common/                     – SDK, events, DTOs
    ├── order-service/              – order creation, Kafka producer
    ├── payment-service/            – payment consumer, JPA, SPI router
    ├── wechat-pay-adapter/         – WeChat Pay implementation
    ├── alipay-adapter/             – Alipay implementation
    ├── docker-compose.yml          – all infrastructure services
    ├── init-scripts/               – PostgreSQL schema init
    ├── build.gradle                – root Gradle build
    ├── settings.gradle             – multi‑module definition
    └── README.md

## Contributing

This project is for learning and portfolio purposes.