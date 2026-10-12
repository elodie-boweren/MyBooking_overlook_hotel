# ADR-0001: Modular Monolith Foundation to Staged Evolutionary Microservices

## Status
**Accepted**

## Context & Problem Statement
The original MyBooking (Overlook Hotel) platform was created as a single Spring Boot application with emerging domain entities (`auth`, `room`, `reservation`, `loyalty`, `employee`, `event`, `feedback`, `installation`, `analytics`). However, premature extraction into distributed microservices without enforcing bounded context boundaries, data ownership, or transaction invariants introduces the classic "distributed monolith" anti-pattern—combining the worst operational overhead of microservices with the coupling of a monolith.

How do we structure the architecture so that the development team can rapidly stabilize business logic, enforce strict domain boundaries, and progressively extract specialized polyglot services (Go, C++, Rust, Java) without disruptive system rewrites?

## Decision Drivers
* **Domain Integrity**: Invariants must be enforced cleanly within business aggregates before introducing network latency and eventual consistency.
* **Operational Simplicity in Early Phases**: Avoid managing distributed tracing, Kafka clusters, and multi-database migrations before core domain logic is verified.
* **Clear Evolutionary Path**: Support extraction of high-throughput services (e.g. Go for availability search, C++20 for yield optimization, Rust for IoT telemetry) without breaking existing workflows.
* **Maintainability & Developer Velocity**: Enable single-command local testing (`./mvnw test`) and deterministic CI feedback.

## Considered Options
1. **Immediate Big-Bang Microservices Extraction**: Decompose the application into 13 separate repositories and microservices on day one.
2. **Permanent Monolith**: Retain a single monolithic application and reject distributed microservices entirely.
3. **Staged Evolutionary Architecture (Modular Monolith to Polyglot Microservices)**: Consolidate the codebase into a disciplined modular monolith with isolated schemas and transactional outbox events (Phases 1–5), followed by staged extraction of targeted services into autonomous polyglot microservices (Phases 6–15).

## Decision Outcome
**Chosen Option**: **Option 3 (Staged Evolutionary Architecture)**.

### Implementation Guidelines
1. **Phases 1–5 (Modular Monolith Baseline)**:
   - Organize domain packages strictly by bounded contexts (`auth`, `room`, `reservation`, `loyalty`, `operations`).
   - Eliminate direct circular dependencies and cross-domain entity joins.
   - Separate PostgreSQL schemas via Flyway migrations (`auth`, `inventory`, `booking`, `loyalty`, `operations`).
   - Decouple asynchronous side-effects using the Transactional Outbox pattern and Spring Application Events.
2. **Phases 6–15 (Staged Polyglot Service Extraction)**:
   - Extract C++20 dynamic pricing via gRPC (Phase 6).
   - Extract Go availability search and edge API gateway (Phase 7).
   - Extract Java identity service with OIDC/OAuth2 (Phase 8).
   - Implement Kafka event streaming and Saga choreography for payment/notifications (Phase 9).
   - Extract Go analytics engine with ClickHouse streaming (Phase 10).
   - Deploy Rust IoT edge gateway, ROS 2 fleet manager, and staff desktop workstation (Phases 12–14).

## Consequences & Operational Impact
* **Positive**:
  - Immediate high test coverage and rapid debugging cycle.
  - Zero premature network failure modes or distributed transaction overhead during core refactoring.
  - Transparent contract definitions that ease extraction when traffic demands it.
* **Negative / Trade-offs**:
  - Requires continuous architectural discipline (package-private scoping, architecture test rules) to avoid context coupling.
  - Schema migration scripts must be designed upfront to prevent monolithic foreign key dependencies.
