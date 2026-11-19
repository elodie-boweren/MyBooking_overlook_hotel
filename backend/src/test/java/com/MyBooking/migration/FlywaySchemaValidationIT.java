package com.MyBooking.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CICD-006: Fail-closed PostgreSQL 16.6-alpine Flyway V1-V8 migration validation gate.
 * Omits disabledWithoutDocker so missing Docker or migration failures fail the CI job.
 */
@Testcontainers
class FlywaySchemaValidationIT {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.6-alpine")
            .withDatabaseName("mybooking_flyway_test")
            .withUsername("test")
            .withPassword("test");

    private static final List<String> EXPECTED_V1_V8_TABLES = List.of(
            "flyway_schema_history",
            "app_user",
            "notification_preference",
            "employee",
            "room",
            "equipment",
            "room_equipment",
            "room_photo",
            "room_status_update",
            "reservation",
            "feedback",
            "feedback_reply",
            "loyalty_account",
            "loyalty_transaction",
            "installation",
            "event",
            "event_booking",
            "announcement",
            "announcement_reply",
            "employee_task",
            "shift",
            "leave_request",
            "training",
            "employee_training",
            "event_notification"
    );

    @Test
    @DisplayName("CICD-006: Flyway migrations V1-V8 apply cleanly from scratch on PostgreSQL 16.6-alpine")
    void flywayMigrationsV1ThroughV8_ShouldApplyCleanlyOnPostgres16() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        MigrateResult result = flyway.migrate();

        assertThat(result.success).isTrue();
        assertThat(result.migrationsExecuted).isGreaterThanOrEqualTo(8);

        MigrationInfo[] applied = flyway.info().applied();
        assertThat(applied).hasSizeGreaterThanOrEqualTo(8);
        assertThat(Arrays.stream(applied).map(MigrationInfo::getState))
                .allMatch(state -> state == MigrationState.SUCCESS);

        Set<String> actualTables = new HashSet<>();
        try (Connection conn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                actualTables.add(rs.getString("table_name"));
            }
        }

        assertThat(actualTables).containsAll(EXPECTED_V1_V8_TABLES);
    }
}
