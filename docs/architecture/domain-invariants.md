# Domain Invariants Catalogue

- **Document Identifier**: `DOC-ARCH-INVARIANTS-001`
- **Authoritative Baseline**: [Reconciled 15-Phase Engineering Roadmap (Trello Backlog Authority)](../roadmap/15-phase-roadmap.md)
- **Status**: **AUTHORITATIVE ARCHITECTURAL STANDARD**

---

## 1. Executive Summary & Purpose

In Domain-Driven Design (DDD), an **invariant** is a business rule or consistency constraint that must remain true at all times within a bounded context. When invariants are violated, system data becomes corrupted, resulting in operational failures (double-booked hotel rooms, overbooked conferences, stolen or negative loyalty balances, staff scheduling conflicts).

This catalogue formalizes the core domain invariants of the MyBooking platform. It specifies:
1. The exact business rules governing each domain aggregate.
2. The specific enforcement layer (Entity, Service, Database constraint, or Transactional Lock).
3. The concurrency requirements necessary to prevent race conditions.
4. Direct traceability to audit findings and roadmap phases.

---

## 2. Invariants Registry by Domain

```
Invariant Index:
┌───────────────┬────────────────────────────────────────────────────────┬──────────────────────┬───────────────┐
│ Invariant ID  │ Title                                                  │ Domain               │ Roadmap Phase │
├───────────────┼────────────────────────────────────────────────────────┼──────────────────────┼───────────────┤
│ INV-RES-001   │ Non-Overlapping Room Reservation Interval              │ Booking / Reservation│ Phase 03      │
│ INV-RES-002   │ Chronological Date Invariant (CheckIn < CheckOut)      │ Booking / Reservation│ Phase 03      │
│ INV-RES-003   │ Reservation State Machine Transition Invariant         │ Booking / Reservation│ Phase 03 / 04 │
│ INV-RES-004   │ 15-Minute Temporary Hold (PENDING_PAYMENT State)       │ Booking / Payment    │ Phase 09      │
│ INV-RES-005   │ 24-Hour Cancellation Policy & Non-Refundable Rates     │ Booking / Pricing    │ Phase 04 / 06 │
│ INV-RES-006   │ Pessimistic Row-Locking Concurrency Boundary           │ Booking / Inventory  │ Phase 03      │
│ INV-ROOM-001  │ Decoupled Room Operational Readiness & Occupancy       │ Inventory / Room     │ Phase 03 / 04 │
│ INV-ROOM-002  │ Immutable Room Identifier & Unique Number Invariant    │ Inventory / Room     │ Baseline / P4 │
│ INV-EVT-001   │ Aggregate Event Participant Capacity Invariant         │ Events & Activities  │ Phase 03      │
│ INV-EVT-002   │ Event Booking Chronological Window Invariant           │ Events & Activities  │ Phase 03      │
│ INV-LOY-001   │ Non-Negative Loyalty Account Balance                   │ Loyalty & Rewards    │ Phase 03      │
│ INV-LOY-002   │ Symmetrical Point Earning & Cancellation Clawback      │ Loyalty & Rewards    │ Phase 03 / 04 │
│ INV-LOY-003   │ Immutable Loyalty Transaction Ledger                   │ Loyalty & Rewards    │ Phase 03 / 04 │
│ INV-EMP-001   │ Employee Leave Exclusivity (No Tasks/Shifts on Leave)  │ Staff Operations     │ Phase 03      │
│ INV-EMP-002   │ Shift Non-Overlap Invariant                            │ Staff Operations     │ Phase 03      │
│ INV-PAY-001   │ Payment-Reservation Balance Equivalence                │ Finance & Billing    │ Phase 09      │
│ INV-IAM-001   │ Cryptographic Principal Integrity (Zero-Trust)         │ Identity & Access    │ Phase 02 (OK) │
│ INV-TEN-001   │ Strict Multi-Property Data Isolation                   │ Multi-Tenancy        │ Phase 04 / 08 │
└───────────────┴────────────────────────────────────────────────────────┴──────────────────────┴───────────────┘
```

---

### Domain: Reservation & Booking Management

#### `INV-RES-001`: Non-Overlapping Room Reservation Interval
* **Business Rule**: For any individual physical room $R$, no two active reservations ($Res_A, Res_B$) may share overlapping calendar dates. A reservation occupies the room during the half-open interval $[CheckIn, CheckOut)$.
  $$\forall Res_A, Res_B \in Reservations(R) \text{ where } Status \in \{CONFIRMED, CHECKED\_IN\}:$$
  $$Res_A.id \ne Res_B.id \implies (Res_A.checkIn \ge Res_B.checkOut \lor Res_A.checkOut \le Res_B.checkIn)$$
  Adjacent back-to-back reservations (where $Res_A.checkOut = Res_B.checkIn$) are strictly permitted.
* **Domain Owner**: `Booking` Bounded Context (`Reservation` Aggregate).
* **Enforcement Layer**:
  - Application Service validation using canonical JPQL interval intersection.
  - Database-level exclusion constraint or transactional pessimistic row locking (`SELECT FOR UPDATE` on `Room`).
* **Concurrency Requirements**: Under concurrent booking requests, in-memory checks are vulnerable to race conditions. The transaction must acquire a lock on the target `Room` before evaluating active interval intersections.
* **Current Status**: **VIOLATED** (Flawed JPQL logic in `ReservationRepository`, missing concurrency locks, missing `CHECKED_IN` status).
* **Audit Finding**: `AUD-RES-001`, `AUD-RES-003`.
* **Roadmap Phase**: **Phase 03**.

#### `INV-RES-002`: Chronological Date Invariant
* **Business Rule**: Check-in date must be strictly before check-out date ($CheckIn < CheckOut$). Check-in date cannot be in the past for new reservations ($CheckIn \ge CurrentDate$). Duration must be at least 1 night.
* **Domain Owner**: `Booking` Bounded Context (`Reservation` Entity).
* **Enforcement Layer**: Entity pre-persist hook and domain validator (`validateReservationDates`).
* **Concurrency Requirements**: Stateless in-memory validation; no concurrency lock required.
* **Current Status**: **SATISFIED** (Enforced in `ReservationService`).
* **Roadmap Phase**: Baseline / Phase 03.

#### `INV-RES-003`: Reservation State Machine Transition Invariant
* **Business Rule**: A reservation must follow deterministic, forward-only lifecycle transitions:
  ```
  [REQUESTED] ──> [CONFIRMED] ──> [CHECKED_IN] ──> [CHECKED_OUT]
         │              │
         ▼              ▼
    [REJECTED]     [CANCELLED]
  ```
  A reservation in status `CANCELLED` or `CHECKED_OUT` is terminal and cannot transition to any active state.
* **Domain Owner**: `Booking` Bounded Context (`Reservation` Aggregate Root).
* **Enforcement Layer**: State pattern or explicit enum state transition guards on `Reservation.transitionTo(ReservationStatus)`.
* **Concurrency Requirements**: Optimistic locking (`@Version`) on `Reservation`.
* **Current Status**: **PARTIALLY VIOLATED** (`ReservationStatus` only has `CONFIRMED` and `CANCELLED`; intermediate stay states are entirely missing).
* **Audit Finding**: `AUD-RES-004`.
* **Roadmap Phase**: **Phase 03** (Enum expansion) / **Phase 04** (Schema Isolation).

#### `INV-RES-004`: 15-Minute Temporary Hold (PENDING_PAYMENT State)
* **Business Rule**: Unpaid online bookings enter status `PENDING_PAYMENT` with a strictly enforced 15-minute expiration timestamp ($ExpiresAt = CreatedAt + 15 \text{ minutes}$). The room inventory is held temporarily. If payment authorization is not captured within 15 minutes, the hold expires and the room is released back to available inventory.
* **Domain Owner**: `Booking` & `Payment` Bounded Contexts.
* **Enforcement Layer**: Redis TTL lease (Phase 07/09) or scheduled expiration worker / Outbox event (Phase 05).
* **Concurrency Requirements**: Distributed lease with TTL.
* **Current Status**: **DOCUMENTED FOR PHASE 09 IMPLEMENTATION** (Documented in `DOC-ARCH-VISION-2026-001`).
* **Audit Finding**: `AUD-RES-004`, `AUD-PAY-001`.
* **Roadmap Phase**: **Phase 09: Distributed Event Streaming & Saga Workflows**.

#### `INV-RES-005`: 24-Hour Cancellation Policy & Non-Refundable Rates
* **Business Rule**: Standard flexible reservations permit full cancellation and refund if cancelled at least 24 hours prior to check-in time ($CancellationTimestamp \le CheckInDate \text{ at 15:00} - 24 \text{ hours}$). Cancellations within 24 hours incur a 1-night forfeit penalty. Non-refundable rate plans and peak-season deposits are strictly non-refundable upon confirmation.
* **Domain Owner**: `Booking` & `Pricing` Bounded Contexts.
* **Enforcement Layer**: Rate plan policy validator in `ReservationService`.
* **Concurrency Requirements**: Standard transaction.
* **Current Status**: **SCHEDULED FOR PHASE 04** (Modeled with rate plans in Phase 04 bounded contexts; integrated with Phase 06 C++ pricing engine).
* **Audit Finding**: Section 4.2 Ambiguous Cancellation Rule.
* **Roadmap Phase**: **Phase 04 (Bounded Contexts)** & **Phase 06 (Dynamic Pricing)**.

#### `INV-RES-006`: Pessimistic Row-Locking Concurrency Boundary
* **Business Rule**: To prevent double-booking under high concurrency while avoiding database bottlenecks:
  1. The transaction acquires a row-level lock on the specific `Room` entity (`SELECT ... FOR UPDATE WHERE id = :roomId`).
  2. Transaction duration must be minimal ($< 5$ ms); zero external HTTP, email, or third-party network calls are permitted inside the lock boundary.
* **Domain Owner**: `Booking` Bounded Context.
* **Enforcement Layer**: JPA / Hibernate `LockModeType.PESSIMISTIC_WRITE` on `RoomRepository.findByIdForUpdate`.
* **Concurrency Requirements**: Row-level database lock.
* **Current Status**: **APPROVED FOR PHASE 03 IMPLEMENTATION**.
* **Audit Finding**: `AUD-RES-003`.
* **Roadmap Phase**: **Phase 03**.

---

### Domain: Room Management & Inventory

#### `INV-ROOM-001`: Decoupled Room Operational Readiness & Occupancy
* **Business Rule**: Physical occupancy, reservation occupancy, and operational cleanliness are orthogonal states:
  1. *Operational Status* (`CLEAN`, `DIRTY`, `INSPECTED`, `OUT_OF_SERVICE`) represents physical room readiness managed by Housekeeping/Maintenance.
  2. *Occupancy State* is dynamically computed for any requested date range from active reservations.
  A room's operational status must **never** be automatically mutated to `OCCUPIED` upon future reservation creation. A room cannot be occupied by a guest unless its operational status is `INSPECTED` or `CLEAN`.
* **Domain Owner**: `Inventory` / `Operations` Bounded Context.
* **Enforcement Layer**: Application Service logic; decoupling `room.status` from `ReservationService`.
* **Concurrency Requirements**: Transaction isolation on room status updates.
* **Current Status**: **CRITICALLY VIOLATED** (`ReservationService` mutates `room.status = OCCUPIED` immediately upon booking creation).
* **Audit Finding**: `AUD-RES-002`, `AUD-ROOM-001`.
* **Roadmap Phase**: **Phase 03**.

---

### Domain: Event Management & Activities

#### `INV-EVT-001`: Aggregate Event Participant Capacity Invariant
* **Business Rule**: The sum of all registered participants across active bookings for an event must never exceed the event's configured maximum capacity:
  $$\sum_{b \in Bookings(Event) \text{ where } Status = CONFIRMED} b.numberOfParticipants \le Event.capacity$$
  Any new booking request where $CurrentConfirmedParticipants + RequestedParticipants > Event.capacity$ must be rejected with a `CapacityExceededException`.
* **Domain Owner**: `Event` Bounded Context (`Event` Aggregate Root).
* **Enforcement Layer**:
  - `EventBookingRepository` aggregation query (`SUM(numberOfParticipants)`).
  - Pessimistic lock (`SELECT ... FOR UPDATE`) on the `Event` record during booking creation to serialize capacity checks.
* **Concurrency Requirements**: Mandatory pessimistic lock on `Event` to prevent parallel requests from oversubscribing capacity.
* **Current Status**: **CRITICALLY VIOLATED** (Only checks single booking size against total capacity; existing bookings are never summed).
* **Audit Finding**: `AUD-EVT-001`, `AUD-EVT-002`.
* **Roadmap Phase**: **Phase 03**.

#### `INV-EVT-002`: Event Booking Chronological Window Invariant
* **Business Rule**: An event booking can only be created for an event scheduled in the future ($Event.startAt > CurrentTimestamp$). An event booking cannot be cancelled after the event start time has passed.
* **Domain Owner**: `Event` Bounded Context.
* **Enforcement Layer**: Entity validation and Application Service checks.
* **Concurrency Requirements**: Standard transaction.
* **Current Status**: **SATISFIED** (Enforced in `EventService.cancelEventBookingAsDto`).
* **Roadmap Phase**: Baseline / Phase 03.

---

### Domain: Loyalty & Rewards

#### `INV-LOY-001`: Non-Negative Loyalty Account Balance
* **Business Rule**: A user's loyalty point balance must never drop below zero:
  $$LoyaltyAccount.balance \ge 0$$
  A redemption request for $P$ points where $P > LoyaltyAccount.balance$ must be rejected immediately with `InsufficientPointsException`.
* **Domain Owner**: `Loyalty` Bounded Context (`LoyaltyAccount` Entity).
* **Enforcement Layer**:
  - Database check constraint: `CHECK (balance >= 0)`.
  - Application Service balance validation.
  - Optimistic locking (`@Version`) on `LoyaltyAccount` to prevent concurrent double-spend race conditions.
* **Concurrency Requirements**: Mandatory `@Version` column on `LoyaltyAccount`.
* **Current Status**: **VIOLATED UNDER CONCURRENCY** (No `@Version` on `LoyaltyAccount`; race condition allows double-spending).
* **Audit Finding**: `AUD-LOY-002`.
* **Roadmap Phase**: **Phase 03**.

#### `INV-LOY-002`: Symmetrical Point Earning & Cancellation Clawback
* **Business Rule**: Loyalty points earned from a reservation must be reversed or clawed back if the associated reservation is cancelled prior to check-in.
  $$Status(Reservation) = CANCELLED \implies \text{Record } LoyaltyTransaction(Type=CLAWBACK, Points=EarnedPoints)$$
  Alternatively, points are only awarded upon completed checkout (`CHECKED_OUT`), eliminating premature point distribution.
* **Domain Owner**: `Loyalty` / `Booking` Bounded Context.
* **Enforcement Layer**: Domain event listener or transactional service coordination.
* **Concurrency Requirements**: Atomic transaction across reservation cancellation and loyalty ledger entry.
* **Current Status**: **CRITICALLY VIOLATED** (Earned points are never clawed back upon cancellation; infinite point generation exploit).
* **Audit Finding**: `AUD-LOY-001`.
* **Roadmap Phase**: **Phase 03** (Lifecycle fix) / **Phase 05** (Domain events).

#### `INV-LOY-003`: Immutable Loyalty Transaction Ledger
* **Business Rule**: `LoyaltyTransaction` records are immutable append-only financial ledger entries. A transaction record cannot be updated or deleted once committed. Account balance is the mathematical sum of all historical ledger entries:
  $$LoyaltyAccount.balance = \sum Tx.points \text{ (where EARN/REFUND is positive, REDEEM/CLAWBACK is negative)}$$
* **Domain Owner**: `Loyalty` Bounded Context.
* **Enforcement Layer**: Database table permissions (no `UPDATE`/`DELETE` grants) and JPA entity configuration (`updatable = false`).
* **Current Status**: **PARTIALLY IMPLEMENTED** (`LoyaltyTransaction` exists, but lacks `CLAWBACK`/`REVERSAL` transaction types).
* **Audit Finding**: `AUD-LOY-001`.
* **Roadmap Phase**: **Phase 03 / 04**.

---

### Domain: Employee Operations & Workforce Scheduling

#### `INV-EMP-001`: Employee Leave Exclusivity (No Tasks or Shifts During Approved Leave)
* **Business Rule**: An employee cannot be scheduled for a `Shift` or assigned an `EmployeeTask` whose scheduled execution date/time overlaps with an approved `LeaveRequest`:
  $$\forall L \in LeaveRequests(Emp) \text{ where } Status = APPROVED:$$
  $$Shift.date \notin [L.fromDate, L.toDate] \land Task.dueDate \notin [L.fromDate, L.toDate]$$
* **Domain Owner**: `Staff Operations` Bounded Context.
* **Enforcement Layer**: Service validation in `createShift`, `updateShift`, and `createTask`.
* **Concurrency Requirements**: Transactional query on `LeaveRequestRepository`.
* **Current Status**: **VIOLATED FOR TASKS** (Shifts check leaves; task assignments completely ignore approved leave).
* **Audit Finding**: `AUD-EMP-001`.
* **Roadmap Phase**: **Phase 03**.

#### `INV-EMP-002`: Shift Non-Overlap Invariant
* **Business Rule**: An employee cannot be assigned two concurrent or overlapping shifts:
  $$\forall S_1, S_2 \in Shifts(Emp): S_1.id \ne S_2.id \implies (S_1.startAt \ge S_2.endAt \lor S_1.endAt \le S_2.startAt)$$
* **Domain Owner**: `Staff Operations` Bounded Context.
* **Enforcement Layer**: `ShiftRepository.findOverlappingShiftsForEmployeeId` check during shift creation and update.
* **Concurrency Requirements**: Transactional validation.
* **Current Status**: **SATISFIED** (Enforced in `EmployeeService.createShift`).
* **Roadmap Phase**: Baseline / Phase 03.

---

### Domain: Payments & Financial Consistency

#### `INV-PAY-001`: Payment-Reservation Balance Equivalence
* **Business Rule**: In the target payment architecture, a reservation's financial status is governed by its payment ledger:
  $$Reservation.financialStatus = \begin{cases} UNPAID & \text{if } TotalPaid = 0 \\ PARTIALLY\_PAID & \text{if } 0 < TotalPaid < TotalPrice \\ SETTLED & \text{if } TotalPaid \ge TotalPrice \end{cases}$$
  No reservation can be checked out with an unpaid debit balance unless guaranteed by an authorized corporate credit line.
* **Domain Owner**: `Payment` Bounded Context (Microservice #7).
* **Enforcement Layer**: Saga orchestration (Phase 09) and Payment ledger.
* **Concurrency Requirements**: Distributed Saga with compensating transactions.
* **Current Status**: **NOT YET IMPLEMENTED** (Monolith has no payment entity; scheduled for Phase 09).
* **Audit Finding**: `AUD-PAY-001`.
* **Roadmap Phase**: **Phase 09: Distributed Event Streaming & Saga Workflows**.

---

### Domain: Multi-Property & Tenant Isolation

#### `INV-TEN-001`: Strict Multi-Property Data Isolation
* **Business Rule**: Every operational resource (`Room`, `Reservation`, `Employee`, `Shift`, `Task`, `Installation`, `Event`) must belong to an explicit `property_id`. A staff member assigned to Property $A$ has zero read or write authority over resources at Property $B$ unless granted platform-wide `SUPER_ADMIN` privileges.
* **Domain Owner**: All Bounded Contexts.
* **Enforcement Layer**: PostgreSQL row-level security / schema isolation (Phase 04) and ABAC/ReBAC policy enforcement (Phase 08 / ADR-0017).
* **Current Status**: **ARCHITECTURAL LIMITATION** (Current monolith assumes single hotel).
* **Audit Finding**: `AUD-TEN-001`.
* **Roadmap Phase**: **Phase 04** (Schema isolation & column additions) / **Phase 08** (Tenant attribute resolver).

---

## 3. Concurrency Invariants Analysis

A critical finding of this audit is that **application-level validation alone is fundamentally insufficient** for invariants operating under concurrent web traffic. The table below details invariants that require database-level or transactional concurrency controls:

| Invariant ID | Target Invariant | Failure Mode Under High Concurrency | Mandatory Concurrency Control |
| :--- | :--- | :--- | :--- |
| `INV-RES-001` | Non-Overlapping Reservation | Two users book the same room simultaneously. Both read `isEmpty() = true`; both insert. Double booking occurs. | **Pessimistic Lock**: `SELECT ... FOR UPDATE` on `Room` row during availability check. |
| `INV-EVT-001` | Event Aggregate Capacity | Two users book remaining seats simultaneously. Both read capacity available; both insert. Overbooking occurs. | **Pessimistic Lock**: `SELECT ... FOR UPDATE` on `Event` row during participant count evaluation. |
| `INV-LOY-001` | Non-Negative Loyalty Balance | Two redemption requests hit parallel threads. Both read balance of 100 points; both deduct 100 points. User spent 200 points. | **Optimistic Lock**: `@Version` column on `LoyaltyAccount`. Second commit fails with `OptimisticLockException`. |
| `INV-EMP-002` | Shift Non-Overlap | Two managers schedule overlapping shifts for the same employee simultaneously. | **Unique Exclusion Index** or serializable transaction boundary on `Shift`. |

---

## 4. Automated Testing Requirements for Invariants

To guarantee that invariants remain intact across refactoring phases, the test harness must include the following test suites:

1. **Reservation Invariant Suite (`ReservationInvariantTest.java`)**:
   - Boundary tests for exact checkout/checkin adjacency (same-day checkout at 11:00 AM, checkin at 3:00 PM).
   - Multi-threaded concurrency simulation (10 parallel threads attempting to book the same room for the same weekend $\rightarrow$ exactly 1 succeeds, 9 receive `RoomNotAvailableException`).
   - Self-conflict update test (changing reservation from 3 nights to 4 nights succeeds without self-blocking).
2. **Event Capacity Invariant Suite (`EventCapacityInvariantTest.java`)**:
   - Sequential bookings summing to capacity (e.g., $4 + 4 + 2 = 10$) $\rightarrow$ exactly reaches capacity.
   - 11th participant booking rejected with `CapacityExceededException`.
   - Parallel concurrent bookings test (5 threads requesting 3 seats on an event with 5 seats remaining $\rightarrow$ only 1 thread succeeds).
3. **Loyalty Concurrency & Ledger Suite (`LoyaltyConcurrencyTest.java`)**:
   - Optimistic lock verification: 2 parallel threads attempting to redeem total balance simultaneously $\rightarrow$ exactly 1 succeeds, 1 receives retryable lock conflict.
   - Lifecycle clawback test: Book reservation $\rightarrow$ earn 100 points $\rightarrow$ cancel reservation $\rightarrow$ balance returns to 0 $\rightarrow$ ledger reflects `CLAWBACK`.
4. **Employee Leave Conflict Suite (`EmployeeLeaveConflictTest.java`)**:
   - Approve vacation for employee $E$ from June 10–20.
   - Attempt to assign task with due date June 15 $\rightarrow$ rejected with `EmployeeOnLeaveException`.

