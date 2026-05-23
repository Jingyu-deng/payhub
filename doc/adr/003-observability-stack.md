# ADR-003: Observability Stack

## Status
Accepted

## Context
PayHub needs observability capabilities to monitor system health, diagnose issues, and understand performance bottlenecks across distributed services. We require metrics collection, visualization, and distributed tracing.

## Decision
We will use the following observability stack:

- **Metrics**: Micrometer (instrumentation) + Prometheus (storage/querying) + Grafana (visualization)
- **Tracing**: Micrometer Tracing (OTel bridge) + OTLP exporter + Jaeger (local trace storage/viewing) → Aliyun ARMS (production)

## Rationale

### Metrics: Micrometer + Prometheus + Grafana (unchanged)
- **Micrometer** is the standard metrics instrumentation library for Spring Boot, providing vendor-neutral APIs.
- **Prometheus** is a battle-tested time-series database with a powerful query language (PromQL).
- **Grafana** provides rich dashboards and supports Prometheus as a data source.
- **Spring Boot Actuator** provides automatic exposure of `/actuator/prometheus` endpoint.

### Tracing: Micrometer Tracing + OTel + Jaeger (updated from Brave/Zipkin)
- **Micrometer Tracing** is the standard tracing abstraction in Spring Boot 3.x.
- **OpenTelemetry (OTel)** is the industry-standard, vendor-neutral observability framework. OTLP is the native protocol for Aliyun ARMS.
- **Jaeger** is used locally as a drop-in OTLP receiver + trace viewer (one container replaces both Zipkin and an OTel Collector).
- In production, the same OTLP exporter sends traces directly to **Aliyun ARMS** — no code changes, just a different endpoint.

### Why not Brave + Zipkin (previous decision)
- Zipkin requires an OTel Collector to bridge between OTLP (the standard protocol) and Zipkin's native format, adding complexity.
- Jaeger accepts OTLP natively and has a comparable UI.
- This eliminates one moving part (the collector) and keeps the local-to-cloud migration surface minimal.

## Consequences
- All services include Micrometer, Prometheus registry, Micrometer Tracing (OTel bridge), and OTLP exporter dependencies.
- Applications expose `/actuator/prometheus` endpoints for Prometheus scraping.
- Tracing context is injected into Kafka message headers via `observation-enabled: true`.
- Docker Compose includes Prometheus, Grafana, and Jaeger containers.
- A single Jaeger container replaces Zipkin + OTel Collector, handling both OTLP ingestion and trace viewing.
- Tracing sampling rate is 1.0 (100%) in the `local` profile and 0.1 (10%) in the `aliyun` profile.
- Spring profiles (`local`, `aliyun`) control the OTLP endpoint; the default is `local`.
- Custom business metrics are registered via Micrometer `MeterRegistry`.
