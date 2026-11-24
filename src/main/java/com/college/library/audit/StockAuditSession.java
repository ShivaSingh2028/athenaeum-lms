package com.college.library.audit;

import com.college.library.tenant.TenantContext;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_audit_sessions")
public class StockAuditSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @Column(nullable = false)
    private String sessionName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StockAuditStatus status = StockAuditStatus.IN_PROGRESS;

    @Column(nullable = false)
    private int scannedCount = 0;

    @Column(nullable = false)
    private int verifiedCount = 0;

    @Column(nullable = false)
    private int misplacedCount = 0;

    @Column(nullable = false)
    private int missingCount = 0;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    private OffsetDateTime completedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StockAuditEntry> entries = new ArrayList<>();

    public StockAuditSession() {}

    public StockAuditSession(String sessionName) {
        this.tenantId = TenantContext.getTenantId();
        this.sessionName = sessionName;
        this.status = StockAuditStatus.IN_PROGRESS;
        this.startedAt = OffsetDateTime.now();
    }

    @PrePersist
    public void prePersist() {
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = TenantContext.getTenantId();
        }
        if (startedAt == null) {
            startedAt = OffsetDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }

    public StockAuditStatus getStatus() { return status; }
    public void setStatus(StockAuditStatus status) { this.status = status; }

    public int getScannedCount() { return scannedCount; }
    public void setScannedCount(int scannedCount) { this.scannedCount = scannedCount; }

    public int getVerifiedCount() { return verifiedCount; }
    public void setVerifiedCount(int verifiedCount) { this.verifiedCount = verifiedCount; }

    public int getMisplacedCount() { return misplacedCount; }
    public void setMisplacedCount(int misplacedCount) { this.misplacedCount = misplacedCount; }

    public int getMissingCount() { return missingCount; }
    public void setMissingCount(int missingCount) { this.missingCount = missingCount; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public List<StockAuditEntry> getEntries() { return entries; }
    public void setEntries(List<StockAuditEntry> entries) { this.entries = entries; }
}
