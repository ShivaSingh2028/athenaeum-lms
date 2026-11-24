-- V2__phase4_search_and_wishlist.sql
-- Athenaeum LMS: Search, Wishlist, and Book Purchase Requests

-- Enable pg_trgm for fuzzy typo-tolerant full-text search
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Trigram GIN indexes for fuzzy search on books
CREATE INDEX IF NOT EXISTS idx_books_title_trgm ON books USING gin (title gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_books_author_trgm ON books USING gin (author gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_books_isbn_trgm ON books USING gin (isbn gin_trgm_ops);

-- Wishlist / Notify Me When Available
CREATE TABLE IF NOT EXISTS wishlists (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    book_id BIGINT NOT NULL REFERENCES books(id) ON DELETE CASCADE,
    notify_on_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_wishlist_user_book UNIQUE (tenant_id, user_id, book_id)
);
CREATE INDEX IF NOT EXISTS idx_wishlists_tenant_user ON wishlists(tenant_id, user_id);
CREATE INDEX IF NOT EXISTS idx_wishlists_book_notify ON wishlists(book_id, notify_on_available);

-- Book Purchase Requests (Patrons suggest acquisitions)
CREATE TABLE IF NOT EXISTS book_requests (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(500) NOT NULL,
    author VARCHAR(500) NOT NULL,
    isbn VARCHAR(50),
    publisher VARCHAR(255),
    reason TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED, ORDERED, ACQUIRED
    admin_notes TEXT,
    reviewed_by_user_id BIGINT REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_book_requests_tenant_status ON book_requests(tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_book_requests_user ON book_requests(user_id);
