package com.college.library.entity;

import com.college.library.tenant.TenantContext;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String tenantId;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @NotBlank
    @Column(nullable = false)
    private String author;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String isbn;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String category;

    private String publisher;

    private Integer publicationYear;

    private String edition;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(length = 1000)
    private String coverImageUrl;

    @Column(length = 50)
    private String language = "English";

    @Transient
    private int totalCopies;

    @Transient
    private int availableCopies;

    @Transient
    private LocalDate addedOn;

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookCopy> copies = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PrePersist
    public void prePersist() {
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = TenantContext.getTenantId();
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

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public Integer getPublicationYear() { return publicationYear; }
    public void setPublicationYear(Integer publicationYear) { this.publicationYear = publicationYear; }

    public String getEdition() { return edition; }
    public void setEdition(String edition) { this.edition = edition; }

    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public List<BookCopy> getCopies() { return copies; }
    public void setCopies(List<BookCopy> copies) { this.copies = copies; }

    public int getTotalCopies() {
        if (copies != null && !copies.isEmpty()) {
            return copies.size();
        }
        return totalCopies;
    }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public int getAvailableCopies() {
        if (copies != null && !copies.isEmpty()) {
            return (int) copies.stream().filter(c -> c.getStatus() == CopyStatus.AVAILABLE).count();
        }
        return availableCopies;
    }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    public LocalDate getAddedOn() {
        return addedOn != null ? addedOn : (createdAt != null ? createdAt.toLocalDate() : LocalDate.now());
    }
    public void setAddedOn(LocalDate addedOn) { this.addedOn = addedOn; }

    public boolean isAvailable() {
        return getAvailableCopies() > 0;
    }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
