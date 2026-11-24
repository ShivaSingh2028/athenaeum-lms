package com.college.library;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Phase 2: PostgreSQL Testcontainers Flyway & Schema Validation Test")
class PostgreSqlFlywayValidationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_athenaeum")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private Flyway flyway;

    @Test
    @DisplayName("Verify Flyway Migrations Applied Cleanly and Tables Exist")
    void testFlywayMigrationsApplied() throws Exception {
        if (!postgres.isRunning()) {
            System.out.println("[SKIP] Docker not running on host; skipped Testcontainers execution.");
            return;
        }

        assertNotNull(dataSource, "DataSource should be auto-configured with Testcontainers");
        assertNotNull(flyway, "Flyway should be present in Spring context");

        // Verify Flyway applied V1, V2, and V3
        var info = flyway.info();
        var applied = info.applied();
        assertTrue(applied.length >= 3, "Expected at least 3 Flyway migrations applied, got: " + applied.length);

        for (var migration : applied) {
            assertEquals("SUCCESS", migration.getState().name(),
                    "Migration " + migration.getScript() + " should be in SUCCESS state");
        }

        // Verify key tables exist in database
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            String[] expectedTables = {
                    "tenants", "tenant_settings", "membership_types", "users",
                    "books", "book_copies", "transactions", "fines", "payments",
                    "reservations", "notifications", "audit_logs", "wishlists",
                    "book_requests", "stock_audit_sessions", "stock_audit_entries"
            };

            for (String table : expectedTables) {
                try (ResultSet rs = stmt.executeQuery("SELECT to_regclass('public." + table + "');")) {
                    assertTrue(rs.next());
                    assertNotNull(rs.getString(1), "Table '" + table + "' must exist in public schema");
                }
            }
        }
    }
}
