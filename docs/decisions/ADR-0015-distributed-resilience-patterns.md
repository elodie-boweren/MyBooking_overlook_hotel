# ADR-0015: Distributed Systems Resilience Patterns (Circuit Breakers, Retries, Backpressure & Timeouts)

## Status
**Accepted**

## Context & Problem Statement
In a distributed microservice topology with 13 services communicating via gRPC, REST, and Kafka, partial network partitions, slow downstream dependencies, or transient database outages can cause cascading failures, thread pool exhaustion, and complete system downtime.

We need standardized architectural resilience patterns across our Java, Go, C++, and Rust services to ensure fault tolerance, predictable degradation, and self-healing operations.

## Decision Drivers
* **Cascading Failure Prevention**: A failure or latency spike in a non-critical service (e.g. `notification-service` or external SMS gateway) must never bring down core reservation booking.
* **Deterministic Resource Consumption**: Bounded thread pools, memory limits, and request timeouts to prevent out-of-memory (OOM) crashes.
* **Thundering Herd Mitigation**: Retrying failed requests simultaneously must not overwhelm recovering services.
* **Data Loss Prevention**: Unprocessable or failed asynchronous messages must be preserved for diagnosis and replay.

## Decision Outcome
We formally adopt the following **Resilience Patterns Standard** across all MyBooking microservices:

### 1. Strict Request Timeouts & Deadlines
* **Rule**: Zero unbounded synchronous network calls. Every HTTP and gRPC client call must specify an explicit timeout/deadline.
* **Standards**:
  * Internal inter-service gRPC calls: **$250\text{ ms}$ timeout**.
  * Dynamic pricing engine calculation: **$50\text{ ms}$ deadline**.
  * External third-party payment/notification calls: **$3,000\text{ ms}$ timeout**.
  * Database queries: **$2,000\text{ ms}$ query timeout**.

### 2. Circuit Breaker Pattern
* **Tooling**: **Resilience4j** for Java/Spring services; **Sony gobreaker** for Go services.
* **Configuration**:
  * Ring buffer size: 100 requests.
  * Failure rate threshold: **$50\%$** (transitions to `OPEN` state).
  * Wait duration in `OPEN` state: **$10\text{ seconds}$** before entering `HALF_OPEN`.
  * Fallback behavior: Graceful degradation (e.g., if `pricing-engine` circuit is open, fallback to pre-cached standard baseline seasonal rates; if `loyalty-service` circuit is open, queue points redemption for asynchronous reconciliation).

### 3. Retries with Exponential Backoff & Full Jitter
* **Rule**: Synchronous mutation calls (e.g. POST booking) are **never** retried blindly without idempotency keys.
* **Algorithm**: For transient network errors (`503 Service Unavailable`, gRPC `UNAVAILABLE`):
  $$t_{\text{sleep}} = \min(t_{\text{max}}, t_{\text{initial}} \times 2^{\text{attempt}}) \times \text{random}(0.5, 1.5)$$
* Maximum retries: **3 attempts**. Jitter prevents the "thundering herd" effect on recovering dependencies.

### 4. Bulkhead Pattern (Resource Isolation)
* **Rule**: Isolate thread pools and connection pools per downstream dependency.
* **Implementation**: In `reservation-service`, payment processing and email notification dispatch execute on dedicated, isolated thread pools with bounded queues. If the payment gateway slows down, front-desk room status reads remain unaffected.

### 5. Backpressure & Bounded Worker Pools
* **Kafka Consumers**: Configured with explicit `max.poll.records` and pause/resume mechanisms to match downstream database write velocity.
* **Go / Rust Edge Services**: Buffered channels with non-blocking drop-or-throttle policies when buffers reach 80% capacity to protect system memory.

### 6. Dead Letter Queue (DLQ) & Poison Pill Handling
* Failed Kafka messages after 3 retry attempts are diverted to a dedicated dead-letter topic (`<topic-name>.DLQ`) accompanied by error headers, stack traces, and correlation IDs for inspection and administrative replay.

## Consequences & Operational Impact
* All microservices include standardized health probes (`/health/liveness`, `/health/readiness`) reflecting circuit breaker states.
* Observability dashboards in Grafana display real-time circuit breaker trip rates, retry counts, and DLQ message volumes.
