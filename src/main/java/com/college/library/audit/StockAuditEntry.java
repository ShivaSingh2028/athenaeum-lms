package com.college.library.audit;

import com.college.library.entity.BookCopy;
import com.college.library.tenant.TenantContext;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "stock_audit_entries")
public class StockAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private StockAuditSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_copy_id", nullable = false)
    private BookCopy bookCopy;

    @Column(nullable = false, length = 100)
    private String barcode;

    @Column(length = 100)
    private String expectedShelfLocation;

    @Column(length = 100)
    private String scannedShelfLocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AuditEntryStatus status = AuditEntryStatus.VERIFIED;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime scannedAt = OffsetDateTime.now();

    public StockAuditEntry() {}

    public StockAuditEntry(StockAuditSession session, BookCopy copy, String barcode,
                           String expectedLocation, String scannedLocation, AuditEntryStatus status) {
        this.tenantId = TenantContext.getTenantId();
        this.session = session;
        this.bookCopy = copy;
        this.barcode = barcode;
        this.expectedShelfLocation = expectedLocation;
        this.scannedShelfLocation = scannedLocation;
        this.status = status;
        this.scannedAt = OffsetDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = TenantContext.getTenantId();
        }
        if (scannedAt == null) {
            scannedAt = OffsetDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public StockAuditSession getSession() { return session; }
    public void setSession(StockAuditSession session) { this.session = session; }

    public BookCopy getBookCopy() { return bookCopy; }
    public void setBookCopy(BookCopy bookCopy) { this.bookCopy = bookCopy; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getExpectedShelfLocation() { return expectedShelfLocation; }
    public void setExpectedShelfLocation(String expectedShelfLocation) { this.expectedShelfLocation = expectedShelfLocation; }

    public String getScannedShelfLocation() { return scannedShelfLocation; }
    public void setScannedShelfLocation(String scannedShelfLocation) { this.scannedShelfLocation = scannedShelfLocation; }

    public AuditEntryStatus getStatus() { return status; }
    public void setStatus(AuditEntryStatus status) { this.status = status; }

    public OffsetDateTime getScannedAt() { return scannedAt; }
    public void setScannedAt(OffsetDateTime scannedAt) { this.scannedAt = scannedAt; }
}
