package com.college.library.tenant;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @Column(length = 64)
    private String id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false, unique = true, length = 100)
    private String subdomain;

    @Column(nullable = false, length = 30)
    private String status = "ACTIVE"; // ACTIVE, SUSPENDED, TRIAL

    @Column(nullable = false, length = 50)
    private String plan = "STANDARD";

    @Column(nullable = false)
    private String contactEmail;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Tenant() {}

    public Tenant(String id, String name, String subdomain, String contactEmail) {
        this.id = id;
        this.name = name;
        this.subdomain = subdomain;
        this.contactEmail = contactEmail;
        this.status = "ACTIVE";
        this.plan = "STANDARD";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSubdomain() { return subdomain; }
    public void setSubdomain(String subdomain) { this.subdomain = subdomain; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
