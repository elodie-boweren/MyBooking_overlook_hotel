# ADR-0002: Single Authoritative Backend Root (`backend/src`) & Test Harness Standards

## Status
**Accepted**

## Context & Problem Statement
Prior to Phase 1, the repository contained a split source layout:
1. An obsolete root `/src` directory containing skeleton files, empty `.gitkeep` markers, starter unit tests, and duplicate configuration files.
2. An active, complete backend codebase situated in `/backend/src`.

This duality caused severe developer confusion:
* IDEs and Maven commands invoked at the project root attempted to compile the skeleton root `/src` rather than the active `/backend/src`.
* Spring Boot 3.5 strict bean overriding defaults caused `@DataJpaTest` slice tests to throw `BeanDefinitionOverrideException` due to duplicate `@EnableJpaRepositories` registrations across test configurations and the application context.
* Hardcoded test identifiers and brittle microsecond timestamp assertions produced intermittent test suite failures.

How do we establish a clean, deterministic repository layout and test harness standard across the MyBooking platform?

## Decision Drivers
* **Single Source of Truth**: There must be exactly one active backend source tree.
* **Seamless Root Lifecycle Execution**: Running `./mvnw clean compile test` from the repository root must work identically to running inside `/backend`.
* **Zero Flaky Tests**: All slice and unit tests must execute deterministically in both local and CI/CD environments.
* **Backward & Multi-Module Compatibility**: The root layout must naturally accommodate future polyglot modules (e.g. Go, Rust, C++) alongside Java `backend`.

## Considered Options
1. **Move everything up to root**: Eliminate the `backend/` folder and move all Java sources directly to root `/src`.
2. **Leave root `src/` and require explicit `-f backend/pom.xml`**: Keep root stubs and force developers to remember `-f backend/pom.xml`.
3. **Consolidate to `backend/src/` with Root Aggregator POM**: Purge root `src/`, establish root `pom.xml` as a multi-module aggregator delegating to `backend`, and enable test bean overriding in test configuration profiles.

## Decision Outcome
**Chosen Option**: **Option 3 (Consolidate to `backend/src/` with Root Aggregator POM)**.

### Architectural Standards Adopted
1. **Directory Consolidation**:
   - Obsolete root `src/` directory is permanently deleted.
   - All backend Java code resides strictly in `backend/src/main/java` and `backend/src/test/java`.
2. **Aggregator Root POM**:
   - The root `pom.xml` is configured with `<packaging>pom</packaging>` and registers `<module>backend</module>`.
   - Running `./mvnw clean`, `./mvnw compile`, and `./mvnw test` from the repository root automatically triggers the Maven Reactor build order across all modules.
3. **Test Harness Configuration**:
   - Set `spring.main.allow-bean-definition-overriding: true` in `application-repository-test.yml` and `application-test.yml` to permit slice test repository isolation without context collisions.
   - Forbid hardcoded primary key identifiers in unit and integration test assertions (dynamic entity IDs must be resolved via getters).
   - Use reasonable timestamp comparison windows (minimum 1–5 seconds) to avoid false negatives due to database clock and nanosecond precision jitter.

## Consequences & Operational Impact
* **Positive**:
  - 100% test pass rate established across all 1,095 automated tests.
  - Simplified CI/CD pipeline definitions (`./mvnw test` at root works out-of-the-box).
  - Clear separation between frontend (`frontend/`), documentation (`docs/`), and backend services.
* **Negative / Trade-offs**:
  - Developers previously navigating to root `src/` must update their IDE project roots to recognize `backend/pom.xml` or root aggregator `pom.xml`.
