package com.college.library;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 0 Foundation & Architecture Smoke Tests")
class FoundationSmokeTest {

    @Test
    @DisplayName("Verify Brand Mark & Favicon Assets Exist")
    void testBrandAssetsExist() {
        assertNotNull(getClass().getResource("/static/images/logo.svg"), "logo.svg must exist");
        assertNotNull(getClass().getResource("/static/images/logo.png"), "logo.png must exist");
        assertNotNull(getClass().getResource("/static/images/icons/favicon-16x16.png"), "favicon 16x16 must exist");
        assertNotNull(getClass().getResource("/static/images/icons/favicon-32x32.png"), "favicon 32x32 must exist");
        assertNotNull(getClass().getResource("/static/images/icons/apple-touch-icon.png"), "apple touch icon must exist");
        assertNotNull(getClass().getResource("/static/images/icons/icon-192x192.png"), "PWA icon 192x192 must exist");
        assertNotNull(getClass().getResource("/static/images/icons/icon-512x512.png"), "PWA icon 512x512 must exist");
        assertNotNull(getClass().getResource("/static/manifest.json"), "PWA manifest.json must exist");
    }

    @Test
    @DisplayName("Verify Brand Design Tokens & Palette Definitions")
    void testDesignTokensPalette() throws Exception {
        InputStream stream = getClass().getResourceAsStream("/static/css/tokens.css");
        assertNotNull(stream, "tokens.css must exist in static assets");
        String content = new String(stream.readAllBytes());
        assertTrue(content.contains("#1b2a4a"), "Navy token #1B2A4A must be present");
        assertTrue(content.contains("#b98d2e"), "Gold token #B98D2E must be present");
        assertTrue(content.contains("#7a1f2b"), "Maroon token #7A1F2B must be present");
        assertTrue(content.contains("#f6f3ec"), "Parchment token #F6F3EC must be present");
        assertTrue(content.contains("[data-theme=\"dark\"]"), "Dark mode tokens must be formalized");
    }

    @Test
    @DisplayName("Verify Flyway V1 Initial Schema Migration Exists & Valid")
    void testFlywayMigrationExists() throws Exception {
        InputStream stream = getClass().getResourceAsStream("/db/migration/V1__init_schema.sql");
        assertNotNull(stream, "V1__init_schema.sql must exist");
        String sql = new String(stream.readAllBytes());
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS tenants"), "Tenants table must be defined");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS book_copies"), "Book copies table must be defined");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS membership_types"), "Membership types table must be defined");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS shedlock"), "ShedLock table must be defined");
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS audit_logs"), "Audit logs table must be defined");
    }

    @Test
    @DisplayName("Verify Docker & Deployment Assets Conformity")
    void testDeploymentAssets() {
        assertTrue(Files.exists(Path.of("Dockerfile")), "Dockerfile must exist");
        assertTrue(Files.exists(Path.of("docker-compose.yml")), "docker-compose.yml must exist");
        assertTrue(Files.exists(Path.of("init-db.sql")), "init-db.sql must exist");
    }
}
