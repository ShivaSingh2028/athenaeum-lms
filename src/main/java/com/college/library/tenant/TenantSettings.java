package com.college.library.tenant;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tenant_settings")
public class TenantSettings {

    @Id
    @Column(length = 64)
    private String tenantId;

    @Column(nullable = false)
    private String libraryName;

    @Column(length = 500)
    private String logoUrl;

    @Column(nullable = false, length = 50)
    private String timezone = "Asia/Kolkata";

    @Column(nullable = false, length = 10)
    private String currency = "INR";

    @Column(nullable = false)
    private boolean allowSelfRegistration = true;

    @Column(nullable = false)
    private boolean requireRegistrationApproval = false;

    @Column(nullable = false)
    private boolean enableTotpForStaff = false;

    @Column(nullable = false, length = 50)
    private String workingDays = "MON,TUE,WED,THU,FRI,SAT";

    @Column(columnDefinition = "TEXT")
    private String holidaysJson = "[]";

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public TenantSettings() {}

    public TenantSettings(String tenantId, String libraryName) {
        this.tenantId = tenantId;
        this.libraryName = libraryName;
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getLibraryName() { return libraryName; }
    public void setLibraryName(String libraryName) { this.libraryName = libraryName; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isAllowSelfRegistration() { return allowSelfRegistration; }
    public void setAllowSelfRegistration(boolean allowSelfRegistration) { this.allowSelfRegistration = allowSelfRegistration; }

    public boolean isRequireRegistrationApproval() { return requireRegistrationApproval; }
    public void setRequireRegistrationApproval(boolean requireRegistrationApproval) { this.requireRegistrationApproval = requireRegistrationApproval; }

    public boolean isEnableTotpForStaff() { return enableTotpForStaff; }
    public void setEnableTotpForStaff(boolean enableTotpForStaff) { this.enableTotpForStaff = enableTotpForStaff; }

    public String getWorkingDays() { return workingDays; }
    public void setWorkingDays(String workingDays) { this.workingDays = workingDays; }

    public String getHolidaysJson() { return holidaysJson; }
    public void setHolidaysJson(String holidaysJson) { this.holidaysJson = holidaysJson; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
