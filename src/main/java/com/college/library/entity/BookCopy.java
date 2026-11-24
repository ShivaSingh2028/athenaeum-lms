package com.college.library.entity;

import com.college.library.tenant.TenantContext;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "book_copies")
public class BookCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String barcode;

    @Column(nullable = false, length = 100)
    private String shelfLocation = "General Rack";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CopyCondition condition = CopyCondition.GOOD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CopyStatus status = CopyStatus.AVAILABLE;

    private BigDecimal price;

    private LocalDate acquisitionDate = LocalDate.now();

    @Version
    private int version = 0;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public BookCopy() {}

    public BookCopy(Book book, String barcode, String shelfLocation, CopyCondition condition) {
        this.book = book;
        this.barcode = barcode;
        this.shelfLocation = shelfLocation != null ? shelfLocation : "General Rack";
        this.condition = condition != null ? condition : CopyCondition.GOOD;
        this.status = CopyStatus.AVAILABLE;
        this.tenantId = book != null ? book.getTenantId() : TenantContext.getTenantId();
    }

    @PrePersist
    public void prePersist() {
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = (book != null && book.getTenantId() != null) ? book.getTenantId() : TenantContext.getTenantId();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public String getShelfLocation() { return shelfLocation; }
    public void setShelfLocation(String shelfLocation) { this.shelfLocation = shelfLocation; }

    public CopyCondition getCondition() { return condition; }
    public void setCondition(CopyCondition condition) { this.condition = condition; }

    public CopyStatus getStatus() { return status; }
    public void setStatus(CopyStatus status) { this.status = status; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public LocalDate getAcquisitionDate() { return acquisitionDate; }
    public void setAcquisitionDate(LocalDate acquisitionDate) { this.acquisitionDate = acquisitionDate; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
