package com.college.library.audit;

import com.college.library.entity.BookCopy;
import com.college.library.repository.BookCopyRepository;
import com.college.library.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StockAuditService {

    private final StockAuditSessionRepository sessionRepository;
    private final StockAuditEntryRepository entryRepository;
    private final BookCopyRepository copyRepository;

    public StockAuditService(StockAuditSessionRepository sessionRepository,
                             StockAuditEntryRepository entryRepository,
                             BookCopyRepository copyRepository) {
        this.sessionRepository = sessionRepository;
        this.entryRepository = entryRepository;
        this.copyRepository = copyRepository;
    }

    public List<StockAuditSession> listSessions() {
        return sessionRepository.findByTenantIdOrderByStartedAtDesc(TenantContext.getTenantId());
    }

    public StockAuditSession getSession(Long id) {
        return sessionRepository.findByIdAndTenantId(id, TenantContext.getTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Stock audit session not found: " + id));
    }

    public List<StockAuditEntry> getSessionEntries(Long sessionId) {
        return entryRepository.findBySessionId(sessionId);
    }

    @Transactional
    public StockAuditSession startSession(String sessionName) {
        StockAuditSession session = new StockAuditSession(sessionName.trim());
        return sessionRepository.save(session);
    }

    @Transactional
    public StockAuditEntry scanCopy(Long sessionId, String barcode, String scannedShelfLocation) {
        StockAuditSession session = getSession(sessionId);
        if (session.getStatus() == StockAuditStatus.COMPLETED) {
            throw new IllegalStateException("Cannot scan items into a completed audit session.");
        }

        String tenantId = TenantContext.getTenantId();
        BookCopy copy = copyRepository.findByTenantIdAndBarcode(tenantId, barcode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Book copy with barcode '" + barcode + "' not found in library."));

        // Check if already scanned
        Optional<StockAuditEntry> existing = entryRepository.findBySessionIdAndBarcode(sessionId, barcode.trim());
        if (existing.isPresent()) {
            return existing.get();
        }

        boolean locationMatches = scannedShelfLocation != null &&
                scannedShelfLocation.trim().equalsIgnoreCase(copy.getShelfLocation() != null ? copy.getShelfLocation().trim() : "");

        AuditEntryStatus status = locationMatches ? AuditEntryStatus.VERIFIED : AuditEntryStatus.MISPLACED;

        StockAuditEntry entry = new StockAuditEntry(
                session, copy, copy.getBarcode(), copy.getShelfLocation(), scannedShelfLocation, status
        );
        StockAuditEntry saved = entryRepository.save(entry);

        // Update counts
        session.setScannedCount(session.getScannedCount() + 1);
        if (status == AuditEntryStatus.VERIFIED) {
            session.setVerifiedCount(session.getVerifiedCount() + 1);
        } else {
            session.setMisplacedCount(session.getMisplacedCount() + 1);
        }
        sessionRepository.save(session);

        return saved;
    }

    @Transactional
    public StockAuditSession finalizeSession(Long sessionId) {
        StockAuditSession session = getSession(sessionId);
        if (session.getStatus() == StockAuditStatus.COMPLETED) {
            return session;
        }

        String tenantId = TenantContext.getTenantId();
        List<BookCopy> allCopies = copyRepository.findByTenantId(tenantId);
        List<StockAuditEntry> scannedEntries = entryRepository.findBySessionId(sessionId);
        Set<String> scannedBarcodes = scannedEntries.stream()
                .map(StockAuditEntry::getBarcode)
                .collect(Collectors.toSet());

        int missingCount = 0;
        for (BookCopy copy : allCopies) {
            if (!scannedBarcodes.contains(copy.getBarcode())) {
                StockAuditEntry missingEntry = new StockAuditEntry(
                        session, copy, copy.getBarcode(), copy.getShelfLocation(), null, AuditEntryStatus.MISSING
                );
                entryRepository.save(missingEntry);
                missingCount++;
            }
        }

        session.setMissingCount(missingCount);
        session.setStatus(StockAuditStatus.COMPLETED);
        session.setCompletedAt(OffsetDateTime.now());
        return sessionRepository.save(session);
    }
}
