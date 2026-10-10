# Bounded Contexts Specification

This document defines the authoritative Bounded Context Map for the MyBooking platform.

```mermaid
contextDiagram
    boundedContext IdentityContext ["Identity & Access Context"]
    boundedContext InventoryContext ["Inventory Context"]
    boundedContext ReservationContext ["Reservation Context"]
    boundedContext PricingContext ["Dynamic Pricing Context"]
    boundedContext LoyaltyContext ["Loyalty Context"]
    boundedContext OperationContext ["Housekeeping & Staff Context"]
    boundedContext IoTContext ["IoT & Smart Room Context"]
    boundedContext FleetContext ["Robot & Drone Fleet Context"]

    IdentityContext --> ReservationContext
    InventoryContext --> ReservationContext
    PricingContext --> ReservationContext
    ReservationContext --> LoyaltyContext
    ReservationContext --> OperationContext
    IoTContext --> OperationContext
    OperationContext --> FleetContext
```

## Bounded Context Catalogue

### 1. Identity & Access Context
* **Responsibility**: Authenticate clients and employees, issue OAuth2/OIDC JWTs, enforce fine-grained RBAC/ABAC policies.
* **Aggregates**: `User`, `Role`, `Permission`, `NotificationPreference`.
* **Invariants**: Email addresses must be unique. Password hashes must use BCrypt/Argon2. Tokens expire in 15 minutes.

### 2. Inventory & Facility Context
* **Responsibility**: Manage room definitions, facility equipment, installation capabilities, maintenance blocks.
* **Aggregates**: `Room`, `Equipment`, `Installation`, `MaintenanceBlock`.
* **Invariants**: Rooms cannot be assigned to overlapping active reservations.

### 3. Reservation Context
* **Responsibility**: Core booking lifecycle (Draft, Confirmed, Checked-In, Checked-Out, Cancelled), stay dates, guest counts.
* **Aggregates**: `Reservation`, `GuestProfile`, `StayRecord`.
* **Invariants**: Check-out date must be strictly after check-in date. Guest count must not exceed room capacity.

### 4. Dynamic Pricing Context
* **Responsibility**: High-speed rate calculation, seasonal pricing rules, demand forecasting, numerical yield optimization.
* **Aggregates**: `RateRule`, `PricingMatrix`, `YieldCalculation`.
* **Invariants**: Price adjustments must remain within property-configured bounds (e.g., ±30% baseline rate).

### 5. Loyalty & Rewards Context
* **Responsibility**: Manage guest reward point accounts, point earn/redeem transactions, tier calculation.
* **Aggregates**: `LoyaltyAccount`, `LoyaltyTransaction`.
* **Invariants**: Loyalty account balance cannot drop below 0. Points redeemed cannot exceed balance.

### 6. Housekeeping & Staff Context
* **Responsibility**: Track room cleanliness status (`DIRTY`, `CLEANING`, `CLEAN`, `INSPECTED`), staff shifts, task assignments.
* **Aggregates**: `HousekeepingTask`, `Shift`, `LeaveRequest`, `Training`.
* **Invariants**: Rooms marked `DIRTY` cannot be assigned to check-in until status transitions to `CLEAN`.

### 7. IoT & Smart Room Context
* **Responsibility**: Edge sensor telemetry (HVAC, motion, occupancy), smart lock BLE/Zigbee command dispatch.
* **Aggregates**: `IoTDevice`, `SensorTelemetry`, `DoorLockCommand`.
* **Invariants**: Unlock commands require valid checked-in reservation credentials.

### 8. Robot & Drone Fleet Context
* **Responsibility**: ROS 2 cleaning and delivery robot mission scheduling, safety geofencing, battery management.
* **Aggregates**: `RobotMission`, `FleetTelemetry`, `SafetyOverride`.
* **Invariants**: Robots must halt velocity commands immediately if human proximity sensors trigger.
