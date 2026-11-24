package com.college.library.communication;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox, Long> {

    @Query("SELECT e FROM EmailOutbox e WHERE e.status = :status AND e.retryCount < e.maxRetries ORDER BY e.createdAt ASC")
    List<EmailOutbox> findPendingEmails(@Param("status") EmailStatus status, Pageable pageable);

    List<EmailOutbox> findByTenantIdOrderByCreatedAtDesc(String tenantId);
}
