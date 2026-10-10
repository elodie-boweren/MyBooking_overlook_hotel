# Database-per-Service & Polyglot Persistence Strategy

## 1. Principles of Data Ownership

1. **Strict Service Boundaries**: Each microservice owns its database. Direct SQL access across service boundaries is strictly prohibited.
2. **Polyglot Selection**: Database technologies are selected according to workload query patterns (ACID relational, high-throughput caching, time-series telemetry, or OLAP analytics).
3. **No Distributed Shared Tables**: Shared static data (e.g. currency codes, room types) are replicated via domain events or cached in localized read-only tables.

## 2. Persistence Map

| Service Name | Database Technology | Access Pattern & Rationale |
| :--- | :--- | :--- |
| `identity-service` | PostgreSQL 14 | Relational ACID, index-heavy user lookup, unique constraint on email. |
| `reservation-service` | PostgreSQL 14 | Relational ACID, date-range overlap queries, strict foreign key constraints. |
| `availability-search` | Redis 7 | High-speed in-memory key-value cache for date availability search indexes. |
| `iot-gateway-service` | TimescaleDB | High-frequency time-series sensor data (HVAC, occupancy, lock heartbeats). |
| `analytics-service` | ClickHouse | Columnar OLAP engine for sub-second analytical queries across millions of events. |
