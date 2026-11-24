package com.college.library.policy;

import com.college.library.tenant.TenantContext;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "membership_types")
public class MembershipType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String code; // UG, PG, FACULTY, STAFF, GUEST

    @NotBlank
    @Column(nullable = false, length = 100)
    private String displayName;

    @Column(nullable = false)
    private int maxBooksAllowed = 3;

    @Column(nullable = false)
    private int loanDurationDays = 14;

    @Column(nullable = false)
    private int renewalLimit = 2;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fineRatePerDay = new BigDecimal("5.00");

    @Column(nullable = false)
    private int gracePeriodDays = 0;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal fineCapAmount = new BigDecimal("500.00");

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public MembershipType() {}

    public MembershipType(String code, String displayName, int maxBooks, int loanDays, int renewalLimit, BigDecimal fineRate) {
        this.code = code;
        this.displayName = displayName;
        this.maxBooksAllowed = maxBooks;
        this.loanDurationDays = loanDays;
        this.renewalLimit = renewalLimit;
        this.fineRatePerDay = fineRate;
        this.tenantId = TenantContext.getTenantId();
    }

    @PrePersist
    public void prePersist() {
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = TenantContext.getTenantId();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public int getMaxBooksAllowed() { return maxBooksAllowed; }
    public void setMaxBooksAllowed(int maxBooksAllowed) { this.maxBooksAllowed = maxBooksAllowed; }

    public int getLoanDurationDays() { return loanDurationDays; }
    public void setLoanDurationDays(int loanDurationDays) { this.loanDurationDays = loanDurationDays; }

    public int getRenewalLimit() { return renewalLimit; }
    public void setRenewalLimit(int renewalLimit) { this.renewalLimit = renewalLimit; }

    public BigDecimal getFineRatePerDay() { return fineRatePerDay; }
    public void setFineRatePerDay(BigDecimal fineRatePerDay) { this.fineRatePerDay = fineRatePerDay; }

    public int getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(int gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; }

    public BigDecimal getFineCapAmount() { return fineCapAmount; }
    public void setFineCapAmount(BigDecimal fineCapAmount) { this.fineCapAmount = fineCapAmount; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
