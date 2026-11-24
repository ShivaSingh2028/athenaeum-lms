package com.college.library.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockAuditSessionRepository extends JpaRepository<StockAuditSession, Long> {
    List<StockAuditSession> findByTenantIdOrderByStartedAtDesc(String tenantId);
    Optional<StockAuditSession> findByIdAndTenantId(Long id, String tenantId);
}
