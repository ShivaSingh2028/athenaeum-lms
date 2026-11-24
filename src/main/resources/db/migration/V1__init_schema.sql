-- ==============================================================================
-- Flyway V1: Initial Enterprise Multi-Tenant Schema
-- Target: PostgreSQL 15+
-- ==============================================================================

-- 1. Tenants (Multi-Tenant Master)
CREATE TABLE IF NOT EXISTS tenants (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    subdomain VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    plan VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    contact_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 2. Tenant Settings
CREATE TABLE IF NOT EXISTS tenant_settings (
    tenant_id VARCHAR(64) PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,
    library_name VARCHAR(255) NOT NULL,
    logo_url VARCHAR(500),
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Kolkata',
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    allow_self_registration BOOLEAN NOT NULL DEFAULT TRUE,
    require_registration_approval BOOLEAN NOT NULL DEFAULT FALSE,
    enable_totp_for_staff BOOLEAN NOT NULL DEFAULT FALSE,
    working_days VARCHAR(50) NOT NULL DEFAULT 'MON,TUE,WED,THU,FRI,SAT',
    holidays_json TEXT DEFAULT '[]',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 3. Membership Types (Policy tiers: UG, PG, Faculty, Staff, Guest)
CREATE TABLE IF NOT EXISTS membership_types (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    max_books_allowed INT NOT NULL DEFAULT 3,
    loan_duration_days INT NOT NULL DEFAULT 14,
    renewal_limit INT NOT NULL DEFAULT 2,
    fine_rate_per_day NUMERIC(10,2) NOT NULL DEFAULT 5.00,
    grace_period_days INT NOT NULL DEFAULT 0,
    fine_cap_amount NUMERIC(10,2) NOT NULL DEFAULT 500.00,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_membership_type_tenant UNIQUE (tenant_id, code)
);

-- 4. Users (Patrons, Librarians, Admins, Super Admins)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    membership_type_id BIGINT REFERENCES membership_types(id),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(30),
    role VARCHAR(50) NOT NULL, -- SUPER_ADMIN, LIBRARY_ADMIN, LIBRARIAN, MEMBER
    member_code VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    two_factor_secret VARCHAR(255),
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMP WITH TIME ZONE,
    reading_history_private BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_users_tenant_email UNIQUE (tenant_id, email),
    CONSTRAINT uq_users_tenant_member_code UNIQUE (tenant_id, member_code)
);
CREATE INDEX IF NOT EXISTS idx_users_tenant_role ON users(tenant_id, role);

-- 5. Bibliographic Book Records (Metadata)
CREATE TABLE IF NOT EXISTS books (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    isbn VARCHAR(20) NOT NULL,
    title VARCHAR(500) NOT NULL,
    author VARCHAR(500) NOT NULL,
    publisher VARCHAR(255),
    publication_year INT,
    edition VARCHAR(50),
    category VARCHAR(100) NOT NULL,
    synopsis TEXT,
    cover_image_url VARCHAR(1000),
    language VARCHAR(50) DEFAULT 'English',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_books_tenant_isbn UNIQUE (tenant_id, isbn)
);
CREATE INDEX IF NOT EXISTS idx_books_tenant_category ON books(tenant_id, category);

-- 6. Physical Book Copies (Discrete Inventory)
CREATE TABLE IF NOT EXISTS book_copies (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    book_id BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    barcode VARCHAR(100) NOT NULL,
    shelf_location VARCHAR(100) NOT NULL DEFAULT 'General Rack',
    condition VARCHAR(50) NOT NULL DEFAULT 'GOOD', -- NEW, GOOD, WORN, DAMAGED
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, ISSUED, RESERVED_HOLD, LOST, DAMAGED, WITHDRAWN
    price NUMERIC(10,2),
    acquisition_date DATE DEFAULT CURRENT_DATE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_copies_tenant_barcode UNIQUE (tenant_id, barcode)
);
CREATE INDEX IF NOT EXISTS idx_copies_tenant_status ON book_copies(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_copies_book_id ON book_copies(book_id);

-- 7. Circulation Transactions (Issues & Returns)
CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    book_copy_id BIGINT REFERENCES book_copies(id),
    book_id BIGINT REFERENCES books(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    issued_by_user_id BIGINT REFERENCES users(id),
    returned_to_user_id BIGINT REFERENCES users(id),
    issue_date DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date DATE NOT NULL,
    return_date DATE,
    renewal_count INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'ISSUED', -- ISSUED, RETURNED, OVERDUE, LOST
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_transactions_tenant_status ON transactions(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_due_date ON transactions(due_date) WHERE status = 'ISSUED';

-- 8. Fines Ledger
CREATE TABLE IF NOT EXISTS fines (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    transaction_id BIGINT NOT NULL REFERENCES transactions(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    amount NUMERIC(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACCRUED', -- ACCRUED, PAID, WAIVED, PARTIALLY_PAID
    paid_amount NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    waived_amount NUMERIC(10,2) NOT NULL DEFAULT 0.00,
    waived_by_user_id BIGINT REFERENCES users(id),
    waiver_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fines_tenant_user_status ON fines(tenant_id, user_id, status);

-- 9. Payments (Online & Cash)
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    fine_id BIGINT REFERENCES fines(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    amount NUMERIC(10,2) NOT NULL,
    payment_method VARCHAR(50) NOT NULL, -- CASH, RAZORPAY_UPI, RAZORPAY_CARD, RAZORPAY_NETBANKING
    payment_status VARCHAR(50) NOT NULL DEFAULT 'COMPLETED', -- PENDING, COMPLETED, FAILED, REFUNDED
    transaction_reference VARCHAR(255),
    order_id VARCHAR(255),
    idempotency_key VARCHAR(255) UNIQUE,
    recorded_by_user_id BIGINT REFERENCES users(id),
    payment_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- 10. Reservations & Holds
CREATE TABLE IF NOT EXISTS reservations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    book_id BIGINT NOT NULL REFERENCES books(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    assigned_copy_id BIGINT REFERENCES book_copies(id),
    reservation_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expiry_date DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, FULFILLED, EXPIRED, CANCELLED
    notification_sent_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_reservations_queue ON reservations(tenant_id, book_id, status, reservation_date);

-- 11. In-App Notifications
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(200),
    message TEXT NOT NULL,
    category VARCHAR(50) DEFAULT 'CIRCULATION', -- CIRCULATION, OVERDUE, HOLD, PAYMENT, SYSTEM
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read);

-- 12. Immutable Audit Log
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    actor_id BIGINT,
    actor_email VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    before_state TEXT,
    after_state TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_time ON audit_logs(tenant_id, created_at DESC);

-- 13. ShedLock Distributed Execution Table
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

-- 14. Email Outbox & Notification Queue
CREATE TABLE IF NOT EXISTS email_outbox (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    body_html TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, SENT, FAILED
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    last_error TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS idx_outbox_pending ON email_outbox(status) WHERE status = 'PENDING';
