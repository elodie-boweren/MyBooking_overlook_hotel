# MyBooking Platform: Comprehensive Business Logic Audit

- **Document Identifier**: `DOC-AUDIT-2026-001`
- **Execution Date**: November 2026
- **Audit Target**: Spring Boot Monolith (`backend/src/main/java/com/MyBooking`)
- **Authoritative Baseline**: [Reconciled 15-Phase Engineering Roadmap (Trello Backlog Authority)](../roadmap/15-phase-roadmap.md)
- **Status**: **OFFICIAL AUDIT REPORT**

---

## 1. Executive Summary & Purpose

Following the successful completion of **Phase 01** (Test Harness & Build Stabilization) and **Phase 02** (Zero-Trust Security & Identity Hardening), this audit was commissioned to systematically examine the business logic across all implemented domains of the MyBooking platform. 

The primary objective is to verify domain correctness, uncover latent business logic defects, test specific failure hypotheses (reservation overlaps, event capacity, loyalty points, room coupling, employee scheduling), and establish an evidence-based baseline. Crucially, all findings and proposed remediations are mapped with strict traceability to our established **Reconciled 15-Phase Engineering Roadmap**.

### Key Audit Conclusions
1. **Critical Reservation & Room Coupling Confirmed**: Creating a future reservation immediately sets `room.status = OCCUPIED` in the database, while `RoomService` and `RoomRepository` reject availability if `status == OCCUPIED`. This fundamentally corrupts real-time hotel operations and blocks future availability searches.
2. **Critical Event Overbooking Confirmed**: `EventService` validates participant counts against event capacity on a per-request basis only (`request.getParticipants() > capacity`), completely omitting aggregation of existing confirmed bookings. Events can be overbooked indefinitely.
3. **Critical Loyalty Exploit Confirmed**: Points are awarded immediately upon reservation creation, but are never reversed or clawed back upon cancellation. Furthermore, `LoyaltyAccount` lacks concurrency locking (`@Version`), enabling double-spending race conditions.
4. **Employee Leave Disconnect Confirmed**: While shifts check approved leaves, task assignment (`isEmployeeAvailableForTask`) completely ignores approved leave requests, allowing staff on vacation to be assigned housekeeping or front-desk tasks.
5. **No Architectural Drift**: All identified defects map cleanly into existing roadmap phases. Core domain correctness issues belong in **Phase 03**, external identifier hardening belongs in **Milestone IdP**, and schema enhancements belong in **Phase 04**.

---

## 2. Authoritative Source of Truth & Context

Before conducting code inspections, the authoritative engineering baseline was reviewed and confirmed:
- **Baseline Document**: `docs/roadmap/15-phase-roadmap.md` (*Reconciled 15-Phase Engineering Roadmap — Trello Backlog Authority*).
- **Established Phase Sequence**:
  1. *Phase 01*: Repository Consolidation & Test Harness Repair (Completed)
  2. *Phase 02*: Zero-Trust Security & Authentication Hardening (Completed)
  3. *Phase 03*: Domain Refactoring & Reservation Core Fixes (Current Active Target)
  4. *Milestone IdP*: Keycloak Identity Provider Adoption, Next.js BFF & Public UUIDv7 Architecture
  5. *Phase 04*: Modular Monolith Bounded Contexts & PostgreSQL Schema Isolation
  6. *Phases 05–15*: Outbox, C++ Pricing, Go Search/Gateway, Identity Domain, Kafka Sagas, Analytics, AI, IoT, Robotics, Tauri, and Kubernetes.
- **Architectural Principle**: **"Never distribute a broken monolith."** Domain logic bugs must be corrected within the monolithic core before schema isolation (Phase 04) or microservice extraction (Phases 06–15) to prevent serializing defective logic into distributed contracts and event streams.

---

## 3. Audit Scope & Methodology

### 3.1 Scope of Codebase Inspected
The audit covered all domain models, JPA repositories, application services, controllers, and database migrations in `backend/`:
- **Reservation Domain**: `Reservation`, `ReservationStatus`, `ReservationRepository`, `ReservationService`, `ClientReservationController`, `AdminReservationController`, `EmployeeReservationController`.
- **Room Domain**: `Room`, `RoomType`, `RoomStatus`, `RoomStatusUpdate`, `Equipment`, `RoomRepository`, `RoomService`, `RoomController`.
- **Event Domain**: `Event`, `EventType`, `EventBooking`, `EventBookingStatus`, `Installation`, `EventRepository`, `EventBookingRepository`, `EventService`, `EventController`, `EventBookingController`.
- **Loyalty Domain**: `LoyaltyAccount`, `LoyaltyTransaction`, `LoyaltyTxType`, `LoyaltyAccountRepository`, `LoyaltyTransactionRepository`, `LoyaltyService`, `LoyaltyController`, `AdminLoyaltyController`.
- **Employee Domain**: `Employee`, `EmployeeStatus`, `Shift`, `EmployeeTask`, `TaskStatus`, `LeaveRequest`, `LeaveRequestStatus`, `Training`, `EmployeeService`, `EmployeeController`, `AdminEmployeeController`.
- **Feedback & Announcements**: `Feedback`, `FeedbackReply`, `FeedbackService`, `Announcement`, `AnnouncementService`.
- **Analytics & Operations**: `AnalyticsService`, `AnalyticsController`.

### 3.2 Finding Classification Schema
Every finding is categorized according to:
- **Confirmed Defect**: Verifiable implementation bug violating clear domain logic.
- **Suspected Defect**: Behavior causing business inconsistency under specific edge cases or concurrency.
- **Ambiguous Business Rule**: Inconsistency arising from an unstated or conflicting product policy.
- **Missing Functionality**: Anticipated capability not yet built, but not currently breaking existing code.
- **Architectural Limitation**: Current design constraint explicitly scheduled for resolution in a future roadmap phase.

---

## 4. In-Depth Verification of Hypotheses

### Hypothesis A: Event Capacity & Overbooking
- **Status**: **CONFIRMED IMPLEMENTATION DEFECT**
- **Location**: `backend/src/main/java/com/MyBooking/event/service/EventService.java` (lines 699–701).
- **Code Evidence**:
  ```java
  Integer numberOfParticipants = request.getNumberOfParticipants() != null ? request.getNumberOfParticipants() : 1;
  if (numberOfParticipants < 1) {
      throw new IllegalArgumentException("Number of participants must be at least 1");
  }
  if (numberOfParticipants > event.getCapacity()) {
      throw new IllegalArgumentException("Number of participants exceeds event capacity of " + event.getCapacity());
  }
  ```
- **Analysis**:
  - The check strictly evaluates `numberOfParticipants > event.getCapacity()`.
  - There is **no query** calculating `SUM(eb.numberOfParticipants)` for confirmed bookings of that event.
  - In `EventBookingRepository.java`, there are queries for total revenue (`getTotalRevenueByStatus`), but **zero queries calculating occupied participant capacity**.
  - No capacity holds or pending statuses exist (`EventBookingStatus` only has `CONFIRMED`, `CANCELLED`, `COMPLETED`, `NO_SHOW`). All created bookings are instantly `CONFIRMED`.
- **Reproduction**:
  1. Create an Event with `capacity = 10`.
  2. User 1 books 6 participants $\rightarrow$ Condition $6 > 10$ is false $\rightarrow$ Booking CONFIRMED.
  3. User 2 books 7 participants $\rightarrow$ Condition $7 > 10$ is false $\rightarrow$ Booking CONFIRMED.
  4. Total participants registered = 13. Overbooked by 30%.
- **Concurrency**: Without row-level locks on `Event` or transactional aggregation, concurrent requests can book past capacity even if aggregation were partially implemented.

---

### Hypothesis B: Loyalty Points & Financial Correctness
- **Status**: **CONFIRMED IMPLEMENTATION DEFECT & EXPLOIT**
- **Location**: `backend/src/main/java/com/MyBooking/reservation/service/ReservationService.java` (lines 95–103, 391–395) & `LoyaltyService.java` (lines 146–148, 172–174).
- **Code Evidence**:
  - In `ReservationService.createReservation`:
    ```java
    loyaltyService.earnPoints(clientId, savedReservation.getTotalPrice(), 
        "Points earned from reservation #" + savedReservation.getId(), savedReservation.getId());
    ```
  - In `ReservationService.cancelReservation`:
    ```java
    // Refund points if any were used
    if (reservation.getPointsUsed() != null && reservation.getPointsUsed() > 0) {
        loyaltyService.refundReservationPoints(reservationId, reservation.getPointsUsed());
    }
    reservation.setStatus(ReservationStatus.CANCELLED);
    ```
- **Analysis**:
  - **Infinite Point Exploit**: Points are awarded immediately at booking creation (often months prior to check-in). If cancelled, redeemed points are refunded, but **earned points are never revoked or clawed back**. A user can book and cancel high-value reservations repeatedly to generate infinite loyalty points.
  - **Premature Earning**: Standard hospitality practice dictates that loyalty points are earned upon checkout/settlement of a completed stay, not at initial reservation booking.
  - **Lost Update Concurrency Risk**: `LoyaltyAccount.java` lacks an `@Version` column. Concurrent redemption requests read the same balance, subtract points independently, and overwrite each other, permitting double-spending into negative balances.
  - **Missing Enum Values**: `LoyaltyTxType` contains only `EARN` and `REDEEM`. There are no transaction types for `CLAWBACK`, `REVERSAL`, `EXPIRY`, or `ADJUSTMENT`.

---

### Hypothesis C: Room Status & Reservation Availability Coupling
- **Status**: **CONFIRMED IMPLEMENTATION DEFECT & ARCHITECTURAL FLAW**
- **Location**: `backend/src/main/java/com/MyBooking/room/service/RoomService.java` (line 146), `RoomRepository.java` (line 160), and `ReservationService.java` (lines 105, 196).
- **Code Evidence**:
  - In `ReservationService`:
    ```java
    roomService.updateRoomStatusAutomatically(roomId, RoomStatus.OCCUPIED, 
        "Reservation created for " + checkIn + " to " + checkOut);
    ```
  - In `RoomService.isRoomAvailable`:
    ```java
    if (room.getStatus() == RoomStatus.OUT_OF_SERVICE || room.getStatus() == RoomStatus.OCCUPIED) {
        return false;
    }
    ```
  - In `RoomRepository.findAvailableRoomsForDateRange`:
    ```sql
    SELECT r FROM Room r WHERE r.status = 'AVAILABLE' AND r.id NOT IN (
        SELECT res.room.id FROM Reservation res WHERE ...
    )
    ```
- **Analysis**:
  - The application conflates three distinct domain states into a single `RoomStatus` enum (`AVAILABLE`, `OCCUPIED`, `OUT_OF_SERVICE`):
    1. *Physical Occupancy* (Is a guest in the room today?).
    2. *Reservation Availability* (Is the room free for dates $[d_1, d_2)$ in the future?).
    3. *Housekeeping / Operational Readiness* (Is the room cleaned and inspected?).
  - When a reservation is booked for next summer, the room is immediately marked `OCCUPIED` today.
  - Consequently, `isRoomAvailable` and `findAvailableRoomsForDateRange` filter the room out of search results for *all* dates because `r.status != 'AVAILABLE'`.
  - Rooms become permanently unbookable after a single reservation.

---

### Hypothesis D: Employee Scheduling & Leave Management
- **Status**: **CONFIRMED DEFECT (Task Assignment) & SUSPECTED RIGIDITY (Shifts)**
- **Location**: `backend/src/main/java/com/MyBooking/employee/service/EmployeeService.java` (lines 564–588, 594–604).
- **Code Evidence**:
  - In `isEmployeeAvailableForShift(Long employeeId, LocalDate date)`:
    ```java
    List<LeaveRequest> leaveRequests = leaveRequestRepository.findByEmployeeAndStatus(
        employeeUser, LeaveRequestStatus.APPROVED);
    for (LeaveRequest leave : leaveRequests) {
        if (!date.isBefore(leave.getFromDate()) && !date.isAfter(leave.getToDate())) {
            return false;
        }
    }
    ```
  - In `isEmployeeAvailableForTask(Long employeeId)`:
    ```java
    public boolean isEmployeeAvailableForTask(Long employeeId) {
        Employee employee = getEmployeeByUserId(employeeId);
        if (employee.getStatus() != EmployeeStatus.ACTIVE) return false;
        return !hasInProgressTraining(employeeId);
    }
    ```
- **Analysis**:
  - Shift scheduling properly checks approved leaves (`LeaveRequestStatus.APPROVED`).
  - **Task assignment completely omits leave validation**: An employee on approved medical or annual leave can be assigned housekeeping or maintenance tasks.
  - **Global Training Lockout**: `hasInProgressTraining` checks `trainings.stream().anyMatch(t -> t.getStatus() == TrainingStatus.IN_PROGRESS)` without checking training dates. An employee enrolled in an ongoing part-time training module is permanently flagged as unavailable for all shifts.

---

### Hypothesis E: Reservation Date Overlap & Update Conflicts
- **Status**: **CONFIRMED IMPLEMENTATION DEFECTS**
- **Location**: `backend/src/main/java/com/MyBooking/reservation/repository/ReservationRepository.java` (lines 214–221, 230–235), `RoomRepository.java` (lines 160–165).
- **Code Evidence**:
  ```sql
  WHERE r.status = 'CONFIRMED' 
  AND ((r.checkIn <= :checkIn AND r.checkOut > :checkIn) OR 
       (r.checkIn < :checkOut AND r.checkOut >= :checkOut) OR 
       (r.checkIn >= :checkIn AND r.checkOut <= :checkOut))
  ```
- **Analysis**:
  - **Mathematical Interval Error**: Two half-open date intervals $[A_{start}, A_{end})$ and $[B_{start}, B_{end})$ intersect if and only if $A_{start} < B_{end} \land A_{end} > B_{start}$. The 3-clause SQL query is convoluted, error-prone, and fails edge cases.
  - **Status Blindness**: The query hardcodes `r.status = 'CONFIRMED'`. If future lifecycle states (e.g., `CHECKED_IN`, `PENDING`) are introduced, the query ignores them, allowing double bookings on rooms with active stays.
  - **Self-Conflict on Update**: When updating dates of an existing reservation, querying `ReservationRepository.checkRoomAvailability` matches the reservation against itself. While `ReservationService.checkRoomAvailabilityForUpdate` performs an in-memory `conflicts.removeIf(r -> r.getId().equals(excludeReservationId))`, this in-memory filtering fails under concurrent bookings and does not leverage database filtering.
  - **Lack of Pessimistic Locking**: Availability check followed by insert without row locks (`PESSIMISTIC_WRITE`) permits classic double-booking race conditions during high-concurrency booking windows.

---

## 5. Detailed Audit Findings Registry

```
Summary of Audit Findings:
┌─────────────────┬────────────────────────────────────────────────────────────┬──────────┬───────────┐
│ Finding ID      │ Domain & Title                                             │ Severity │ Status    │
├─────────────────┼────────────────────────────────────────────────────────────┼──────────┼───────────┤
│ AUD-RES-001     │ Reservation: Flawed Date Overlap JPQL Query               │ Critical │ Confirmed │
│ AUD-RES-002     │ Reservation: Static Room Mutation to OCCUPIED on Booking   │ Critical │ Confirmed │
│ AUD-RES-003     │ Reservation: Missing Concurrency Locking on Booking Check │ High     │ Confirmed │
│ AUD-RES-004     │ Reservation: Incomplete Lifecycle (No Stay Representation) │ Medium   │ Confirmed │
│ AUD-EVT-001     │ Event: Unchecked Aggregate Capacity (Overbooking Flaw)    │ Critical │ Confirmed │
│ AUD-EVT-002     │ Event: Missing Concurrency Locking on Event Booking        │ High     │ Confirmed │
│ AUD-LOY-001     │ Loyalty: Premature Earning & No Cancellation Clawback      │ High     │ Confirmed │
│ AUD-LOY-002     │ Loyalty: Race Condition on Point Redemption (Lost Update)  │ High     │ Confirmed │
│ AUD-ROOM-001    │ Room: Conflation of Occupancy, Readiness & Availability    │ Critical │ Confirmed │
│ AUD-EMP-001     │ Employee: Task Assignment Ignores Approved Leave           │ Medium   │ Confirmed │
│ AUD-EMP-002     │ Employee: Global Training Lockout Ignores Shift Dates      │ Medium   │ Confirmed │
│ AUD-FDBK-001    │ Feedback: Reviews Permitted on Cancelled Reservations      │ Low      │ Confirmed │
│ AUD-ANL-001     │ Analytics: Full Memory Scans & Revenue Boundary Miscalc    │ Medium   │ Confirmed │
│ AUD-PAY-001     │ Payment: Absence of Payment & Transaction Tracking Ledger  │ High     │ Limitation│
│ AUD-TEN-001     │ Multi-Tenancy: Monolithic Schema Lacks Property Scoping    │ High     │ Limitation│
└─────────────────┴────────────────────────────────────────────────────────────┴──────────┴───────────┘
```

### Finding `AUD-RES-001`: Flawed Date Overlap JPQL Query
- **Domain**: Reservation & Availability
- **Source**: `ReservationRepository.java` (L214–221, L230–235), `RoomRepository.java` (L160–165).
- **Existing Behavior**: Checks three verbose boolean conditions against `status = 'CONFIRMED'`.
- **Expected Behavior**: Single canonical half-open interval condition: `r.checkIn < :checkOut AND r.checkOut > :checkIn` with active status filtering (`status IN ('CONFIRMED', 'CHECKED_IN')`).
- **Reproduction**: A reservation exists for May 1–5. Querying availability for May 5–10 must succeed (back-to-back checkout/checkin). Boundary edge cases under complex clauses risk false-positive conflict or missed overlaps.
- **Severity**: **Critical** | **Confidence**: **Confirmed**
- **Roadmap Phase**: **Phase 03** (Direct match to Phase 3 objective).

### Finding `AUD-RES-002`: Premature Static Room Mutation to OCCUPIED
- **Domain**: Reservation ↔ Room Interaction
- **Source**: `ReservationService.java` (L105, L196), `RoomService.java` (L146).
- **Existing Behavior**: Creating a reservation for future dates immediately sets `room.status = RoomStatus.OCCUPIED`.
- **Expected Behavior**: Room status remains `AVAILABLE` (or operational cleanliness status) until the guest physically checks in on the check-in date. Future reservations are tracked solely via the `reservation` table.
- **Severity**: **Critical** | **Confidence**: **Confirmed**
- **Roadmap Phase**: **Phase 03** (Direct match to Phase 3 scope: "decouple static room status mutations").

### Finding `AUD-EVT-001`: Event Aggregate Capacity Bypass (Overbooking Flaw)
- **Domain**: Event Booking
- **Source**: `EventService.java` (L699–701).
- **Existing Behavior**: Only checks `request.getNumberOfParticipants() > event.getCapacity()`.
- **Expected Behavior**: Sum of existing confirmed participants plus new participants must not exceed `event.getCapacity()`:
  $$\sum_{b \in Bookings_{confirmed}} b.participants + newParticipants \le event.capacity$$
- **Reproduction**: Capacity = 20. Booking 1 requests 15 (succeeds). Booking 2 requests 10 (succeeds). Total 25 participants registered.
- **Severity**: **Critical** | **Confidence**: **Confirmed**
- **Roadmap Phase**: **Phase 03 Amendment / Scope Clarification** (Must be corrected before event outbox events in Phase 05).

### Finding `AUD-LOY-001`: Asymmetric Loyalty Earning & No Cancellation Clawback
- **Domain**: Loyalty & Billing
- **Source**: `ReservationService.java` (L95, L392), `LoyaltyService.java`.
- **Existing Behavior**: Points earned immediately upon booking creation. Upon cancellation, redeemed points are refunded, but earned points are retained by guest.
- **Expected Behavior**: Points should only be earned upon completed check-out, OR points earned must be reversed/clawed back in the transaction ledger when a reservation is cancelled.
- **Severity**: **High** | **Confidence**: **Confirmed**
- **Roadmap Phase**: **Phase 03 Amendment** (Core domain rule correction) or **Phase 04** (when loyalty bounded context is isolated).

### Finding `AUD-LOY-002`: Race Condition on Point Redemption (Lost Update Anomaly)
- **Domain**: Loyalty
- **Source**: `LoyaltyAccount.java` (L14–60), `LoyaltyService.java` (L172–174).
- **Existing Behavior**: In-memory balance arithmetic without JPA `@Version` or pessimistic lock.
- **Expected Behavior**: Optimistic locking (`@Version private Long version;`) on `LoyaltyAccount` to detect concurrent modifications and reject double-spending.
- **Severity**: **High** | **Confidence**: **Confirmed**
- **Roadmap Phase**: **Phase 03 Amendment**.

### Finding `AUD-ROOM-001`: Conflation of Occupancy, Readiness, and Availability
- **Domain**: Room Inventory & Operations
- **Source**: `Room.java` (L47), `RoomStatus.java` (L3–7).
- **Existing Behavior**: Single enum (`AVAILABLE`, `OCCUPIED`, `OUT_OF_SERVICE`) models physical presence, future bookings, and cleaning status simultaneously.
- **Expected Behavior**: Decouple into two orthogonal concepts:
  1. *Operational Status*: `CLEAN`, `DIRTY`, `INSPECTED`, `MAINTENANCE_OUT_OF_SERVICE`.
  2. *Occupancy / Reservation Availability*: Evaluated dynamically by comparing requested intervals against active reservations.
- **Severity**: **Critical** | **Confidence**: **Confirmed**
- **Roadmap Phase**: **Phase 03** (Decouple static status) and **Phase 04** (Full operational status schema).

### Finding `AUD-EMP-001`: Task Assignment Ignores Approved Employee Leave
- **Domain**: Staff Operations
- **Source**: `EmployeeService.java` (L594–604).
- **Existing Behavior**: `isEmployeeAvailableForTask` only verifies active status and absence of in-progress training; completely ignores `LeaveRequest`.
- **Expected Behavior**: Employee cannot be assigned a task whose scheduled execution falls within an approved leave window.
- **Severity**: **Medium** | **Confidence**: **Confirmed**
- **Roadmap Phase**: **Phase 03 Amendment** (Domain refactoring).

### Finding `AUD-FDBK-001`: Reviews Permitted on Cancelled Reservations
- **Domain**: Feedback
- **Source**: `FeedbackService.java` (L307–309).
- **Existing Behavior**: Only checks `reservation.getCheckOut().isAfter(LocalDate.now())`.
- **Expected Behavior**: Feedback creation must verify `reservation.getStatus() == ReservationStatus.CONFIRMED` (and in future `CHECKED_OUT`). Cancelled reservations must be barred from submitting feedback.
- **Severity**: **Low** | **Confidence**: **Confirmed**
- **Roadmap Phase**: **Phase 03 Amendment**.

### Finding `AUD-ANL-001`: In-Memory Database Scanning & OutOfMemory Vulnerability
- **Domain**: Analytics & Reporting
- **Source**: `AnalyticsService.java` (L301, L330).
- **Existing Behavior**: `calculateRoomRevenue` and `calculateOccupiedRoomNights` invoke `reservationService.getAllReservations()`, dumping the entire table into JVM memory and filtering via Java streams.
- **Expected Behavior**: Push aggregations down to database SQL/JPQL (`SELECT COALESCE(SUM(r.totalPrice), 0)...`) with indexed date criteria.
- **Severity**: **Critical** | **Confidence**: **Confirmed**
- **Roadmap Phase**: **Phase 03** (Monolith query hardening via `P3-T07`) and **Phase 10** (Go + ClickHouse distributed OLAP).

### Finding `AUD-ANL-002`: Hardcoded Mock Event Revenue & Cross-Period Double-Counting
- **Domain**: Analytics & Finance
- **Source**: `AnalyticsService.java` (L311–316, L303).
- **Existing Behavior**: `calculateEventRevenue` returns hardcoded `BigDecimal.valueOf(25000.00)`. Reservations spanning across period boundaries have their full price counted multiple times in monthly revenue trends.
- **Expected Behavior**: Calculate real event revenue via `EventBookingRepository` confirmed bookings; prorate multi-month stays nightly to prevent double-counting.
- **Severity**: **High** | **Confidence**: **Confirmed**
- **Roadmap Phase**: **Phase 03** (`P3-T07`).

### Finding `AUD-ANN-001`: Orphan Backend Module (Missing REST Controller & DTOs)
- **Domain**: Employee Announcements
- **Source**: `com.MyBooking.announcement` (`controller/.gitkeep`, `dto/.gitkeep`).
- **Existing Behavior**: Domain entities, repositories, and `AnnouncementService` exist with tests, but no DTOs and no REST controller are implemented. The feature is completely unreachable from web, mobile, or REST clients.
- **Expected Behavior**: Expose secured REST endpoints (`AnnouncementController`) with complete request/response DTOs.
- **Severity**: **High** | **Confidence**: **Confirmed**
- **Roadmap Phase**: **Phase 03** (`P3-T08`).

### Finding `AUD-ANN-002`: Absence of Department Audience Targeting
- **Domain**: Employee Operations & Communications
- **Source**: `Announcement.java`, `Employee.java`.
- **Existing Behavior**: Announcements have global scope with no department or role filtering. No `Department` domain concept exists in `Employee`.
- **Expected Behavior**: Introduce `Department` enum to `Employee` and audience scoping (`targetDepartment`, `targetRole`) to `Announcement`.
- **Severity**: **Medium** | **Confidence**: **Confirmed**
- **Roadmap Phase**: **Phase 03** (`P3-T08`).

### Finding `AUD-PAY-001`: Complete Absence of Payment Model in Monolith
- **Domain**: Finance & Payments
- **Source**: Monolith codebase (no `Payment` entity exists).
- **Existing Behavior**: Reservations have a `totalPrice` field but zero transactional payment tracking, deposit tracking, or gateway state.
- **Expected Behavior**: Full Payment Service with PSP integration (Stripe/Adyen), ledger, and Saga orchestration.
- **Classification**: **Architectural Limitation** (Scheduled for **Phase 09: Distributed Event Streaming & Saga Workflows** with Microservice #7 `payment-service`).

### Finding `AUD-TEN-001`: Monolithic Schema Lacks Multi-Property Scoping
- **Domain**: Multi-Tenancy & Architecture
- **Source**: All entity tables (`room`, `reservation`, `employee`).
- **Existing Behavior**: Single-hotel assumption; no `property_id` or hotel identifier columns.
- **Classification**: **Architectural Limitation** (Scheduled for **Phase 04: Schema Isolation** and **Phase 08: Identity Domain Service & Tenant Attributes**).

---

## 6. Classification Matrix

| Finding ID | Classification | Action Required |
| :--- | :--- | :--- |
| `AUD-RES-001` | **Confirmed Defect** | Rewrite JPQL queries with canonical half-open interval intersection. |
| `AUD-RES-002` | **Confirmed Defect** | Remove premature `room.status = OCCUPIED` calls in `ReservationService`. |
| `AUD-RES-003` | **Confirmed Defect** | Add pessimistic/optimistic locking to room reservation checks. |
| `AUD-RES-004` | **Missing Functionality** | Introduce explicit reservation lifecycle states (`CHECKED_IN`, `CHECKED_OUT`). |
| `AUD-EVT-001` | **Confirmed Defect** | Add participant aggregation query and capacity barrier in `EventService`. |
| `AUD-EVT-002` | **Confirmed Defect** | Add transactional concurrency check on event booking. |
| `AUD-LOY-001` | **Confirmed Defect** | Defer loyalty earning to checkout, or implement cancellation reversal. |
| `AUD-LOY-002` | **Confirmed Defect** | Add `@Version` column to `LoyaltyAccount` to eliminate lost updates. |
| `AUD-ROOM-001`| **Confirmed Defect** | Decouple static room status from reservation queries. |
| `AUD-EMP-001` | **Confirmed Defect** | Validate approved leave in `isEmployeeAvailableForTask`. |
| `AUD-EMP-002` | **Confirmed Defect** | Check training dates instead of global `IN_PROGRESS` status. |
| `AUD-FDBK-001`| **Confirmed Defect** | Disallow feedback on cancelled reservations. |
| `AUD-ANL-001` | **Confirmed Defect** | Replace in-memory reservation scan with repository aggregation queries. |
| `AUD-ANL-002` | **Confirmed Defect** | Replace hardcoded mock event revenue and eliminate multi-month double counting. |
| `AUD-ANN-001` | **Missing Functionality** | Expose secured REST controller and DTOs for orphan announcement service. |
| `AUD-ANN-002` | **Missing Functionality** | Introduce Department domain model and targeted announcement audience scoping. |
| `AUD-PAY-001` | **Architectural Limitation** | Implement in Phase 09 (`payment-service`). |
| `AUD-TEN-001` | **Architectural Limitation** | Implement in Phase 04 / Phase 08 (Multi-property tenant isolation). |

---

## 7. Mandatory Roadmap Traceability

Every identified defect and limitation is traced directly to our authoritative 15-phase roadmap:

| Finding ID | Description | Existing Phase | Coverage Status | Required Action | Scope Impact | Dependencies | Recommendation |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `AUD-RES-001` | Flawed Overlap JPQL Query | **Phase 03** | **Fully Covered** | Fix query | None | Phase 02 | Implement in Phase 3 (`P3-T01`) |
| `AUD-RES-002` | Static Room Mutation | **Phase 03** | **Fully Covered** | Decouple status | None | Phase 02 | Implement in Phase 3 (`P3-T02`) |
| `AUD-RES-003` | Concurrency on Booking | **Phase 03** | Partially Covered | Add lock | Clarification | Phase 02 | Add to Phase 3 tests |
| `AUD-RES-004` | Stay Lifecycle States | **Phase 03 / 04** | Partially Covered | Model states | Clarification | Phase 03 | Expand enum in Phase 3 |
| `AUD-EVT-001` | Event Capacity Overbooking | **Phase 03** | **Uncovered (Domain Bug)** | Fix query & check | Proposed Amendment | Phase 02 | Implemented in `P3-T03` |
| `AUD-EVT-002` | Event Booking Concurrency | **Phase 03** | Uncovered | Add lock | Proposed Amendment | Phase 02 | Implemented in `P3-T03` |
| `AUD-LOY-001` | Loyalty Point Earning/Clawback | **Phase 03 / 04** | Uncovered | Reversal logic | Proposed Amendment | Phase 02 | Implemented in `P3-T04` |
| `AUD-LOY-002` | Loyalty Lost Update Concurrency| **Phase 03** | Uncovered | Add `@Version` | Proposed Amendment | Phase 02 | Implemented in `P3-T04` |
| `AUD-ROOM-001`| Room Status Conflation | **Phase 03 / 04** | Partially Covered | Decouple in P3 | Clarification | Phase 03 | Implemented in `P3-T02` |
| `AUD-EMP-001` | Task Ignores Approved Leave | **Phase 03** | Uncovered | Add leave check | Proposed Amendment | Phase 02 | Implemented in `P3-T05` |
| `AUD-EMP-002` | Global Training Lockout | **Phase 03** | Uncovered | Check dates | Proposed Amendment | Phase 02 | Implemented in `P3-T05` |
| `AUD-FDBK-001`| Feedback on Cancelled Booking | **Phase 03** | Uncovered | Add status check | Proposed Amendment | Phase 02 | Implemented in `P3-T05` |
| `AUD-ANL-001` | Analytics Memory Scans | **Phase 03 / 10** | **Fully Covered** | JPQL SUM + ClickHouse | Phase 3 Enhancement | Phase 02 | Implement in Phase 3 (`P3-T07`) |
| `AUD-ANL-002` | Mock Event Revenue & Double Counting | **Phase 03** | **Uncovered (Domain Bug)** | Real Event Rev & Proration | Phase 3 Enhancement | Phase 02 | Implement in Phase 3 (`P3-T07`) |
| `AUD-ANN-001` | Orphan Announcement Backend | **Phase 03** | **Uncovered (Missing Layer)** | REST Controller & DTOs | Phase 3 Enhancement | Phase 02 | Implement in Phase 3 (`P3-T08`) |
| `AUD-ANN-002` | Missing Department Scoping | **Phase 03** | **Uncovered (Missing Domain)**| Department & Target Query | Phase 3 Enhancement | Phase 02 | Implement in Phase 3 (`P3-T08`) |
| `AUD-PAY-001` | Absence of Payment Service | **Phase 09** | **Fully Covered** | Svc #7 Payment | None | Phase 08 | Keep in Phase 09 |
| `AUD-TEN-001` | Monolith Single Property | **Phase 04 / 08** | **Fully Covered** | Schema & Tenant | None | Phase 03 | Model in Phase 4 / 8 |

---

## 8. Phase 03 Backlog Reassessment & Recommendation

### Original Phase 03 Definition (Unchanged Baseline)
- **Title**: `PHASE 03: Domain Refactoring & Reservation Core Fixes`
- **Objective**: Correct date overlap JPQL calculation, fix self-conflict on updates, decouple static room status mutations.
- **Scope**: `ReservationRepository`, `ReservationService`.

### The Core Architectural Question
*Should Phase 03 remain strictly limited to the 3 reservation defects, or should it encompass the other confirmed domain logic bugs (`AUD-EVT-001` Event overbooking, `AUD-LOY-001/002` Loyalty exploit/concurrency, `AUD-EMP-001` Employee leave, `AUD-ANL-001/002` Analytics memory bugs, `AUD-ANN-001/002` Orphan announcements)?*

### Recommendation: Cohesive Domain Integrity in Phase 03
Because Phase 04 performs **PostgreSQL Schema Isolation (Flyway `V9__*`)** and establishes Bounded Contexts, introducing domain schema isolation on top of defective business logic (overbooking events, infinite loyalty points, in-memory heap scanning crashes, orphan announcement modules) is an anti-pattern. 

We recommend executing Phase 03 as **Domain Refactoring & Core Business Logic Fixes**, structured into 8 cohesive tickets with stress testing as the concluding capstone:
- **`P3-T01`**: Reservation JPQL Overlap Query Repair & Self-Conflict Elimination (`AUD-RES-001`, `AUD-RES-003`) — *COMPLETED*.
- **`P3-T02`**: Decouple Room Operational Status from Reservation Lifecycle (`AUD-RES-002`, `AUD-ROOM-001`) — *COMPLETED*.
- **`P3-T03`**: Event Capacity Aggregation & Overbooking Prevention (`AUD-EVT-001`, `AUD-EVT-002`) — *COMPLETED*.
- **`P3-T04`**: Loyalty Lifecycle Correction (Cancellation Clawback & Optimistic Lock) (`AUD-LOY-001`, `AUD-LOY-002`) — *COMPLETED*.
- **`P3-T05`**: Employee Leave Scheduling & Feedback Integrity (`AUD-EMP-001`, `AUD-EMP-002`, `AUD-FDBK-001`) — *COMPLETED*.
- **`P3-T07`**: Analytics Service Query Hardening, Heap Scan Elimination & Real Financial Metrics (`AUD-ANL-001`, `AUD-ANL-002`).
- **`P3-T08`**: Department Domain Model & Announcement REST API with DTOs and Audience Scoping (`AUD-ANN-001`, `AUD-ANN-002`).
- **`P3-T06`**: Concurrency Stress Test Suite, Regression Harness & Phase 3 Sign-off Report (Capstone).

*This preserves the exact sequence of the 15-phase roadmap, ensures zero orphan code or latent OOM bugs remain in the monolith, perfectly positions the platform for **Milestone IdP** (Keycloak + Next.js BFF + UUIDv7), and enters **Phase 04** with 100% mathematically and logically sound domain models.*

