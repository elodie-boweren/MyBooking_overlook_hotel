# ADR-0014: Technology Selection of Go (Golang) for Analytics Service

## Status
**Accepted**

## Context & Problem Statement
The platform requires a dedicated `analytics-service` to process operational events (room bookings, housekeeping changes, IoT energy metrics, front-desk activity) and provide real-time and historical Business Intelligence metrics (RevPAR, ADR, occupancy rates, staff efficiency) for property managers.

We evaluated whether `analytics-service` should be implemented in **C++20**, **Java/Spring Boot 3**, or **Go (Golang)**.

## Decision Drivers
* **Event Ingestion Throughput**: Must consume high-volume Kafka event streams and execute batch inserts into our columnar analytical database (**ClickHouse**).
* **ClickHouse Integration**: Native driver efficiency and TCP protocol performance.
* **Query Latency**: Sub-250ms analytical aggregation API response times for manager dashboards.
* **Resource Footprint**: Minimal memory and CPU consumption while maintaining high concurrent request handling.
* **Developer Velocity & Maintainability**: Clean, lightweight codebase without excessive boilerplate or manual memory management.

## Considered Options
1. **C++20**: Extreme execution speed, zero runtime overhead.
2. **Java 17 / Spring Boot 3**: Consistency with core transactional services.
3. **Go (Golang)**: Lightweight concurrency (goroutines), fast compiled binaries, native ClickHouse streaming drivers.

## Decision Outcome
**Chosen Option: Go (Golang)**

### Justification:
1. **ClickHouse Handles the Heavy Numerical Math**: Unlike the `pricing-engine` (which executes custom combinatorial matrix algorithms in memory), `analytics-service` delegates heavy columnar aggregations directly to ClickHouse (which is already implemented in optimized vectorized C++).
2. **Stream Processing Efficiency**: Go's native concurrency model (goroutines and channels) allows high-throughput buffering, batching, and streaming of Kafka events directly into ClickHouse with minimal memory overhead (<30MB baseline RAM).
3. **Avoids Unnecessary C++ Complexity**: Implementing Kafka consumers and HTTP/gRPC dashboard APIs in C++ introduces significant manual memory management, threading, and library integration complexity without any measurable performance advantage over ClickHouse's internal vectorized execution.
4. **Superior to Java for Stream Batching**: Avoids JVM heap overhead and GC tuning challenges during bursty analytical event ingestion.

## Consequences & Operational Impact
* `analytics-service` will be developed in Go using `clickhouse-go` (v2 native protocol) and `segmentio/kafka-go`.
* C++20 remains strictly dedicated to `pricing-engine`, where in-memory matrix optimization genuinely requires manual memory control and zero GC pauses.
* `analytics-service` exposes high-performance gRPC and REST endpoints to the Tauri workstation and Next.js frontends.
