# Architectural Decisions: Security, Concurrency, Performance & Resilience

This document defines the binding architectural decisions, engineering patterns, resilience mechanics, and performance budgets to ensure MyBooking is **highly secure, concurrent, high-performing, and resilient**.

---

## 1. High Security: Zero-Trust Architectural Baseline

### 1.1 Elimination of Client-Supplied Identity (Zero IDOR / BOLA)
* **Rule**: HTTP headers such as `X-User-Id` supplied by the client are strictly forbidden for authorization decisions.
* **Mechanism**: Every incoming request must supply an `Authorization: Bearer <JWT>` header. The `edge-api-gateway` and backend microservices cryptographically verify the signature using asymmetric RS256/ECDSA public keys.
* **Principal Resolution**: Controllers inject `@AuthenticationPrincipal UserPrincipal principal`, whose fields (`id`, `email`, `roles`, `propertyIds`) are populated strictly from the verified JWT payload.

### 1.2 Token Lifecycle & Instant Revocation
* **Short-Lived Access Tokens**: Lifetime of 15 minutes.
* **Refresh Token Rotation (RTR)**: Long-lived refresh tokens (7 days) stored exclusively in `HttpOnly`, `Secure`, `SameSite=Strict` cookies. Each refresh request issues a new refresh token and invalidates the previous one.
* **Redis Token Blacklist**: Instant logout or administrative account suspension adds the token `jti` (JWT ID) to a Redis blacklist with a TTL equal to the token's remaining lifespan.

### 1.3 Service-to-Service Zero-Trust (mTLS & SPIFFE/SPIRE)
* Internal microservice-to-microservice calls do not rely on network perimeter security.
* In Kubernetes, inter-service gRPC/REST traffic is encrypted using **Mutual TLS (mTLS)** with cryptographic service identities issued by **SPIFFE/SPIRE**.

### 1.4 AI Agent & Physical Safety Guardrails
* **No Direct DB Access**: AI models and agents never receive database credentials or direct SQL/ORM access.
* **Deterministic Tool Gateway**: AI actions execute through versioned, strictly typed gRPC APIs. Each tool call is authorized against the invoking user's RBAC/ABAC context.
* **Human-in-the-Loop**: High-consequence operations (e.g. rate changes over ±20%, physical lock overrides, robot mission overrides) require explicit manager confirmation.

### 1.5 DevSecOps Security Automation
* **Pre-commit**: GitLeaks prevents secrets and private keys from entering Git history.
* **CI/CD Pipeline**: 
  * Static Application Security Testing (SAST) via SonarQube / Semgrep.
  * Software Composition Analysis (SCA) via OWASP Dependency-Check.
  * Container vulnerability scanning via Trivy.
  * Image signing via Cosign before pushing to the private container registry.
  * Kyverno admission controllers in Kubernetes to enforce signed, non-root container images.

---

## 2. High Concurrency: Zero Double-Booking & Consistency Guarantees

### 2.1 The Double-Booking Race Condition Problem
In high-demand periods (flash sales, holiday seasons), multiple guests attempt to book the exact same room for overlapping dates simultaneously.

### 2.2 Multi-Tiered Concurrency Controls

```
                        Incoming Booking Request
                                   │
                                   ▼
          ┌─────────────────────────────────────────────────┐
          │  Tier 1: Redis Distributed Lock (Redlock)       │
          │  Key: `lock:room:{propertyId}:{roomId}:{dates}` │
          └────────────────────────┬────────────────────────┘
                                   │ Lock Acquired (TTL 5s)
                                   ▼
          ┌─────────────────────────────────────────────────┐
          │  Tier 2: Database Row-Level Pessimistic Lock    │
          │  `SELECT ... FROM room WHERE id = :id FOR UPDATE`│
          └────────────────────────┬────────────────────────┘
                                   │
                                   ▼
          ┌─────────────────────────────────────────────────┐
          │  Tier 3: Overlap Verification & State Transition│
          │  `r.checkIn < :checkOut AND r.checkOut > :checkIn`│
          └────────────────────────┬────────────────────────┘
                                   │
                                   ▼
          ┌─────────────────────────────────────────────────┐
          │  Tier 4: Optimistic Entity Versioning (@Version)│
          │  Detects concurrent mutations at commit time    │
          └─────────────────────────────────────────────────┘
```

1. **Tier 1 — Redis Distributed Lock**: 
   * When `reservation-service` processes a booking draft, it acquires a distributed lock in Redis keyed by `lock:room:{roomId}:{checkIn}:{checkOut}`.
   * Competing requests fail fast with `409 Conflict` ("Room is currently being reserved by another guest") rather than waiting on database locks.
2. **Tier 2 — Database Pessimistic Row Lock (`FOR UPDATE`)**:
   * Ensures that within the PostgreSQL transaction, the room record is locked against concurrent updates.
3. **Tier 3 — Correct Date Overlap JPQL Query**:
   * Interval overlap condition: `r.checkIn < :newCheckOut AND r.checkOut > :newCheckIn`.
   * Correctly permits back-to-back bookings (e.g., Guest A checks out Oct 10, Guest B checks in Oct 10).
4. **Tier 4 — Optimistic Locking (`@Version`)**:
   * JPA entities include a `@Version Long version` column. Any concurrent update detected at flush time throws an `OptimisticLockException` and triggers an automatic retry or safe error response.

### 2.3 Idempotency Key Architecture
* Financial mutations and booking creation endpoints require an `X-Idempotency-Key: <UUID>` header.
* The key is stored in Redis with its processing status (`PROCESSING`, `COMPLETED`) and cached response body. Retried network calls receive the cached response without re-executing charges or creating duplicate reservations.

---

## 3. High Performance: Global Latency Budgets & Workload Allocation

### 3.1 Measurable Latency & Throughput Budgets

| Operation | Target p95 Latency | Target p99 Latency | Concurrency / Throughput Target | Technology |
| :--- | :--- | :--- | :--- | :--- |
| **Room Availability Search** | $< 25\text{ ms}$ | $< 50\text{ ms}$ | $5,000\text{ requests/sec}$ | Go + Redis Key-Value Cache |
| **Dynamic Yield Pricing Matrix** | $< 10\text{ ms}$ | $< 15\text{ ms}$ | $2,000\text{ evaluations/sec}$ | C++20 gRPC Engine |
| **Reservation Creation (ACID)** | $< 60\text{ ms}$ | $< 100\text{ ms}$ | $500\text{ bookings/sec}$ | Java / PostgreSQL 14 |
| **IoT Telemetry Ingestion** | $< 5\text{ ms}$ | $< 10\text{ ms}$ | $20,000\text{ sensor events/sec}$ | Rust + TimescaleDB |
| **Analytics Query (RevPAR/ADR)**| $< 100\text{ ms}$ | $< 250\text{ ms}$ | Analytical OLAP across 10M rows | Go + ClickHouse Columnar DB |

### 3.2 Architectural Drivers for High Performance
1. **Polyglot Workload Matching**:
   * **C++20 for Dynamic Pricing**: Heavy mathematical combinatorial matrix solving requires pure CPU/memory bandwidth with zero JVM or Go GC pauses.
   * **Go for Availability Search & Gateway**: Non-blocking I/O and goroutines handle tens of thousands of concurrent guest search connections with small memory footprints (< 30MB base RAM).
   * **Go for Analytics Stream Ingestion**: Fast Kafka batch consumer writing directly into ClickHouse native TCP protocol without object allocation overhead.
   * **Rust for Edge IoT & Robotics**: Native real-time sensor processing and serial/BLE lock communication without unpredictable GC pauses.
2. **Database-per-Service Decoupling**: Eliminates global database lock contention.
3. **CQRS for Inventory**: Write path mutates PostgreSQL; read path queries pre-warmed Redis bitsets populated via Kafka events.

---

## 4. Distributed Systems Resilience & Fault Tolerance Patterns (ADR-0015)

To guarantee that failures in one component do not cascade across the 13 microservices, the following patterns are enforced across all services:

### 4.1 Strict Timeouts & Deadlines
* **Rule**: Zero unbounded synchronous network calls.
* **Standards**:
  * Internal inter-service gRPC calls: **$250\text{ ms}$ timeout**.
  * Dynamic pricing engine calculation: **$50\text{ ms}$ deadline**.
  * External payment/SMS calls: **$3,000\text{ ms}$ timeout**.
  * Database queries: **$2,000\text{ ms}$ query timeout**.

### 4.2 Circuit Breakers & Graceful Degradation
* **Tooling**: **Resilience4j** (Java), **Sony gobreaker** (Go).
* **Thresholds**: 50% failure rate over 100 requests trips circuit to `OPEN` for 10 seconds.
* **Fallbacks**: If `pricing-engine` is unavailable, `reservation-service` falls back to baseline seasonal rates; if `loyalty-service` trips, point deductions are queued for asynchronous reconciliation.

### 4.3 Retries with Exponential Backoff & Full Jitter
* **Rule**: Retries apply strictly to idempotent operations and transient network failures (`503`, `UNAVAILABLE`).
* **Formula**:
  $$t_{\text{sleep}} = \min(t_{\text{max}}, t_{\text{initial}} \times 2^{\text{attempt}}) \times \text{random}(0.5, 1.5)$$
* Maximum retries: 3 attempts.

### 4.4 Bulkheads (Resource Isolation)
* Critical workflows maintain dedicated, isolated thread and connection pools. A slow external notification provider cannot exhaust the thread pool handling front-desk check-in operations.

### 4.5 Backpressure & Poison Pill Handling (DLQ)
* Kafka consumers use bounded concurrency and pause/resume logic to match downstream write speeds.
* Failed messages after 3 retries are published to `<topic>.DLQ` with error metadata, preventing poison-pill pipeline stalls.
