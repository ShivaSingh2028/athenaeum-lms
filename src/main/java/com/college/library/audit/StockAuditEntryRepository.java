package com.college.library.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockAuditEntryRepository extends JpaRepository<StockAuditEntry, Long> {
    List<StockAuditEntry> findBySessionId(Long sessionId);
    List<StockAuditEntry> findBySessionIdAndStatus(Long sessionId, AuditEntryStatus status);
    Optional<StockAuditEntry> findBySessionIdAndBarcode(Long sessionId, String barcode);
    boolean existsBySessionIdAndBarcode(Long sessionId, String barcode);
}
