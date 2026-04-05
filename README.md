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
- **Persistent storage** using PostgreSQL and JPA
- **Full containerisation** with Docker Compose
- **Production‑ready configuration** for real‑world deployment

This project is designed as a portfolio piece for senior/architect roles, showcasing both code quality and architectural documentation (C4 diagrams, ADRs).

## Tech Stack

| Component          | Technology                                   |
|--------------------|----------------------------------------------|
| Language           | Java 21                                      |
| Framework          | Spring Boot 3.2.4, Spring Data JPA          |
| Message Broker     | Apache Kafka (with Kafdrop UI)              |
| Cache / Lock       | Redis (Redisson for distributed locks)      |
| Database           | PostgreSQL 15                                |
| Build Tool         | Gradle (multi‑module)                        |
| Containerisation   | Docker, Docker Compose                       |
| Observability      | Micrometer, Prometheus, Grafana (planned)   |
| Testing            | JUnit 5, Mockito, Testcontainers            |

## Architecture

![C4 Container Diagram](docs/c4-container-diagram.png)

The system consists of:
- **Order Service** – REST API to create orders, persists order, publishes `OrderCreatedEvent` to Kafka.
- **Payment Service** – consumes `OrderCreatedEvent`, processes payment (mock), records payment in DB.
- **Kafka** – asynchronous event bus.
- **Redis** – idempotency store and distributed lock manager.
- **PostgreSQL** – stores orders, payments, audit logs.

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

## Project Status

| Feature | Status |
|---------|--------|
| Docker Compose (Kafka, Redis, PostgreSQL) | ✅ Done |
| Order Service REST API | ✅ Done |
| Kafka producer / consumer | ✅ Done |
| JPA + PostgreSQL persistence | ✅ Done |
| Manual offset commit | ✅ Done |
| Redis idempotency (duplicate prevention) | 🔄 In progress |
| Distributed locks (Redisson) | ⏳ Planned |
| Kafka reliability (acks=all, idempotence) | ⏳ Planned |
| Dead‑letter queue (DLQ) | ⏳ Planned |
| Unit / integration tests | ⏳ Planned |
| Prometheus + Grafana | ⏳ Planned |
| Kubernetes deployment (Minikube) | ⏳ Planned |
| Load testing (JMeter) | ⏳ Planned |

## Architecture Decisions (ADRs)

- [ADR‑001](docs/adr/001-message-queue.md) – Why Kafka instead of RabbitMQ or RocketMQ.
- ADR‑002 (planned) – Distributed lock strategy (Redisson vs RedisTemplate).
- ADR‑003 (planned) – Kafka partition count and consumer concurrency.

## Project Structure

    payhub/
    ├── common/                     – shared DTOs, events
    ├── order-service/              – order creation, Kafka producer
    ├── payment-service/            – payment consumer, JPA
    ├── docker-compose.yml          – all infrastructure services
    ├── init-scripts/               – PostgreSQL schema init
    ├── build.gradle                – root Gradle build
    ├── settings.gradle             – multi‑module definition
    └── README.md

## Contributing

This project is for learning and portfolio purposes.
