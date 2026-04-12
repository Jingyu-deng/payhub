# ADR‑002: Distributed Lock for Inventory Deduction

## Status
Accepted

## Context
We need to prevent overselling when multiple concurrent payment requests try to deduct stock for the same product. The system is distributed (multiple instances of payment service may run).

## Decision
Use **Redisson** with Redis as the distributed lock implementation.

## Rationale
- **Simplicity**: Redisson provides a familiar `RLock` interface with automatic lock renewal (watchdog).
- **Reliability**: Handles lock timeouts, deadlock detection, and is widely used in production.
- **Performance**: Redis is already used for idempotency; no new infrastructure.
- **Alternative considered**: Redis native `SET NX` with Lua scripts – more complex and error‑prone.

## Consequences
- Redisson dependency added.
- Lock keys follow pattern `lock:stock:<productId>`.
- Lock wait time = 5 seconds, lease time = 10 seconds.
- Stock is deducted inside the lock, ensuring atomicity.