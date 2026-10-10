# Distributed Transactions & Saga Architecture

## 1. Overview
In a database-per-service architecture, business operations crossing service boundaries cannot use traditional 2-Phase Commit (2PC) due to availability and locking overhead. MyBooking utilizes **Saga Choreography and Orchestration** patterns with eventual consistency.

## 2. Representative Saga Workflows

```mermaid
sequenceDiagram
    autonumber
    participant Guest as Web Client
    participant Booking as reservation-service
    participant Pricing as pricing-engine
    participant Loyalty as loyalty-service
    participant Payment as payment-service
    participant Kafka as Kafka Broker

    Guest->>Booking: POST /api/reservations
    Booking->>Booking: Create Draft (PENDING)
    Booking->>Kafka: ReservationDrafted

    Kafka->>Pricing: Consume: ReservationDrafted
    Pricing->>Kafka: PriceCalculated

    Kafka->>Loyalty: Consume: PriceCalculated
    Loyalty->>Loyalty: Reserve Points
    Loyalty->>Kafka: PointsReserved

    Kafka->>Payment: Consume: PointsReserved
    Payment->>Payment: Charge Credit Card
    alt Payment Succeeded
        Payment->>Kafka: PaymentAuthorized
        Kafka->>Booking: Consume: PaymentAuthorized -> CONFIRMED
    else Payment Failed
        Payment->>Kafka: PaymentFailed
        Kafka->>Loyalty: Consume: PaymentFailed -> Release Points (Compensating Tx)
        Kafka->>Booking: Consume: PaymentFailed -> CANCELLED
    end
```

## 3. Compensation & Reconciliation
* Every action step in a Saga must define a corresponding **Compensating Transaction** (e.g. `ReservePoints` -> `ReleasePoints`).
* An automated background **Reconciliation Scheduler** periodically compares pending draft reservations older than 15 minutes and issues cancellation compensating events if payment was never received.
