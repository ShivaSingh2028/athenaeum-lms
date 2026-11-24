-- V3__phase6_stock_verification.sql
-- Athenaeum LMS: Physical Stock Verification and Inventory Audits

CREATE TABLE IF NOT EXISTS stock_audit_sessions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    session_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, COMPLETED
    scanned_count INT NOT NULL DEFAULT 0,
    verified_count INT NOT NULL DEFAULT 0,
    misplaced_count INT NOT NULL DEFAULT 0,
    missing_count INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_stock_audit_tenant ON stock_audit_sessions(tenant_id, started_at DESC);

CREATE TABLE IF NOT EXISTS stock_audit_entries (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    session_id BIGINT NOT NULL REFERENCES stock_audit_sessions(id) ON DELETE CASCADE,
    book_copy_id BIGINT NOT NULL REFERENCES book_copies(id) ON DELETE CASCADE,
    barcode VARCHAR(100) NOT NULL,
    expected_shelf_location VARCHAR(100),
    scanned_shelf_location VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'VERIFIED', -- VERIFIED, MISPLACED, MISSING
    scanned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_entries_session ON stock_audit_entries(session_id, status);
CREATE INDEX IF NOT EXISTS idx_audit_entries_barcode ON stock_audit_entries(tenant_id, barcode);
