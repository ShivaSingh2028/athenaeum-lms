package com.college.library.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
    List<AuditLog> findByTenantIdAndEntityNameAndEntityIdOrderByCreatedAtDesc(String tenantId, String entityName, String entityId);
}
