package com.college.library.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRequestRepository extends JpaRepository<BookRequest, Long> {
    List<BookRequest> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<BookRequest> findByTenantIdAndUserIdOrderByCreatedAtDesc(String tenantId, Long userId);
    List<BookRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, BookRequestStatus status);
}
