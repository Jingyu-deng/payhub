# ADR‑001: Message Queue Selection

## Status
Accepted

## Context
PayHub needs to decouple order creation from payment processing. We require a message broker that can handle high throughput, provide message durability, and allow replayability for audit and recovery.

## Decision
We will use **Apache Kafka** as our message broker.

## Rationale
- **Throughput**: Kafka is built for high‑volume, low‑latency streaming.
- **Durability & Replay**: Messages are persisted to disk and retained for a configurable period, enabling replay in case of failures or for analytics.
- **Ordering Guarantees**: Kafka preserves order within a partition, which allows us to process related events sequentially.
- **Spring Integration**: Spring Kafka provides excellent support, simplifying development.
- **Ecosystem**: Kafka has a large community, rich tooling (e.g., Kafdrop), and is widely adopted.

Alternatives considered:
- **RabbitMQ**: Suitable for task queues but lacks the same throughput and replay capabilities.
- **RocketMQ**: Similar features, but Kafka’s ecosystem is more mature and better aligned with our team’s experience.

## Consequences
- We must learn Kafka concepts (topics, partitions, consumer groups) – which we have already started.
- Operational complexity increases (broker configuration, partition planning, monitoring).
- Retention policies must be tuned to balance storage cost with business needs.