package com.college.library;

import com.college.library.audit.*;
import com.college.library.entity.*;
import com.college.library.fake.*;
import com.college.library.finance.Fine;
import com.college.library.payment.Payment;
import com.college.library.payment.PaymentMethod;
import com.college.library.payment.PaymentStatus;
import com.college.library.privacy.PatronDataExportDto;
import com.college.library.privacy.PrivacyService;
import com.college.library.service.ReportService;
import com.college.library.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReportsAndPrivacyTest {

    private static final String TENANT_ID = "t-reports-univ";

    private FakeBookRepository bookRepo;
    private FakeBookCopyRepository copyRepo;
    private FakeUserRepository userRepo;
    private FakeTransactionRepository transactionRepo;
    private FakeFineRepository fineRepo;
    private FakePaymentRepository paymentRepo;
    private FakeAuditLogRepository auditLogRepo;
    private FakeStockAuditSessionRepository auditSessionRepo;
    private FakeStockAuditEntryRepository auditEntryRepo;

    private ReportService reportService;
    private StockAuditService stockAuditService;
    private AuditLogService auditLogService;
    private PrivacyService privacyService;

    private User sampleUser;
    private Book b1;
    private Book b2;
    private BookCopy copy1;
    private BookCopy copy2;
    private BookCopy copy3;

    @BeforeEach
    public void setup() {
        TenantContext.setTenantId(TENANT_ID);

        bookRepo = new FakeBookRepository();
        copyRepo = new FakeBookCopyRepository();
        userRepo = new FakeUserRepository();
        transactionRepo = new FakeTransactionRepository();
        fineRepo = new FakeFineRepository();
        paymentRepo = new FakePaymentRepository();
        auditLogRepo = new FakeAuditLogRepository();
        auditSessionRepo = new FakeStockAuditSessionRepository();
        auditEntryRepo = new FakeStockAuditEntryRepository();

        reportService = new ReportService(bookRepo, copyRepo, userRepo, transactionRepo);
        stockAuditService = new StockAuditService(auditSessionRepo, auditEntryRepo, copyRepo);
        auditLogService = new AuditLogService(auditLogRepo);
        privacyService = new PrivacyService(userRepo, transactionRepo, fineRepo, paymentRepo, auditLogService, new BCryptPasswordEncoder());

        sampleUser = new User("Alan Turing", "alan.turing@example.edu", "secret", Role.MEMBER);
        sampleUser.setId(10L);
        sampleUser.setTenantId(TENANT_ID);
        sampleUser.setPhone("+91-9876543210");
        sampleUser.setMemberCode("MEM-ALAN-01");
        userRepo.save(sampleUser);

        b1 = new Book();
        b1.setId(100L);
        b1.setTitle("Computability and Logic");
        b1.setAuthor("George Boolos");
        b1.setIsbn("9780521876803");
        b1.setTenantId(TENANT_ID);
        bookRepo.save(b1);

        b2 = new Book();
        b2.setId(200L);
        b2.setTitle("The Universal Computer");
        b2.setAuthor("Martin Davis");
        b2.setIsbn("9781568811253");
        b2.setTenantId(TENANT_ID);
        bookRepo.save(b2);

        copy1 = new BookCopy(b1, "BC-AUDIT-001", "Rack A", CopyCondition.GOOD);
        copy1.setId(1L);
        copy1.setTenantId(TENANT_ID);
        copyRepo.save(copy1);

        copy2 = new BookCopy(b1, "BC-AUDIT-002", "Rack A", CopyCondition.GOOD);
        copy2.setId(2L);
        copy2.setTenantId(TENANT_ID);
        copyRepo.save(copy2);

        copy3 = new BookCopy(b2, "BC-AUDIT-003", "Rack B", CopyCondition.GOOD);
        copy3.setId(3L);
        copy3.setTenantId(TENANT_ID);
        copyRepo.save(copy3);
    }

    @AfterEach
    public void teardown() {
        TenantContext.clear();
    }

    @Test
    public void testDeadStockAndOverdueAgingAnalysis() throws Exception {
        // Loan out copy1 of b1, but b2 is never loaned out (Dead Stock)
        Transaction tx = new Transaction(copy1, sampleUser, LocalDate.now().minusDays(15), LocalDate.now().minusDays(5));
        tx.setId(501L);
        tx.setTenantId(TENANT_ID);
        tx.setStatus(TransactionStatus.ISSUED);
        transactionRepo.save(tx);

        // Verify dead stock
        List<Book> deadStock = reportService.deadStockBooks();
        assertEquals(1, deadStock.size());
        assertEquals("The Universal Computer", deadStock.get(0).getTitle());

        // Verify aging
        List<ReportService.AgingBucket> aging = reportService.overdueAgingAnalysis();
        assertEquals(3, aging.size());
        // 5 days overdue falls into bucket "1 - 7 Days"
        assertEquals(1, aging.get(0).count());

        // Verify PDF report generation
        byte[] pdf = reportService.generateCirculationReportPdf();
        assertNotNull(pdf);
        assertTrue(pdf.length > 500);
        String header = new String(pdf, 0, 4);
        assertEquals("%PDF", header, "Should be valid PDF format");
    }

    @Test
    public void testStockVerificationAuditSessionLifecycle() {
        // Start Audit Session
        StockAuditSession session = stockAuditService.startSession("Annual Verification 2026");
        assertNotNull(session.getId());
        assertEquals(StockAuditStatus.IN_PROGRESS, session.getStatus());

        // 1. Scan copy1 with matching shelf location (Rack A)
        StockAuditEntry entry1 = stockAuditService.scanCopy(session.getId(), "BC-AUDIT-001", "Rack A");
        assertEquals(AuditEntryStatus.VERIFIED, entry1.getStatus());

        // 2. Scan copy2 with mismatched shelf location (Found on Rack C instead of Rack A)
        StockAuditEntry entry2 = stockAuditService.scanCopy(session.getId(), "BC-AUDIT-002", "Rack C");
        assertEquals(AuditEntryStatus.MISPLACED, entry2.getStatus());

        // 3. Finalize session: copy3 was never scanned, so it should be flagged MISSING
        StockAuditSession finalized = stockAuditService.finalizeSession(session.getId());
        assertEquals(StockAuditStatus.COMPLETED, finalized.getStatus());
        assertEquals(2, finalized.getScannedCount());
        assertEquals(1, finalized.getVerifiedCount());
        assertEquals(1, finalized.getMisplacedCount());
        assertEquals(1, finalized.getMissingCount());

        List<StockAuditEntry> allEntries = stockAuditService.getSessionEntries(session.getId());
        assertEquals(3, allEntries.size());
        assertTrue(allEntries.stream().anyMatch(e -> e.getBarcode().equals("BC-AUDIT-003") && e.getStatus() == AuditEntryStatus.MISSING));
    }

    @Test
    public void testPatronPersonalDataExportDPDP() {
        // Add transaction and fine for user
        Transaction tx = new Transaction(copy1, sampleUser, LocalDate.now().minusDays(10), LocalDate.now().plusDays(5));
        tx.setId(701L);
        tx.setTenantId(TENANT_ID);
        transactionRepo.save(tx);

        Fine fine = new Fine(tx, sampleUser, BigDecimal.valueOf(50.00));
        fine.setId(801L);
        fine.setTenantId(TENANT_ID);
        fineRepo.save(fine);

        Payment payment = new Payment(fine, sampleUser, BigDecimal.valueOf(50.00), PaymentMethod.CASH, "TXN-CASH-001");
        payment.setId(901L);
        payment.setTenantId(TENANT_ID);
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        paymentRepo.save(payment);

        // Export data
        PatronDataExportDto export = privacyService.exportPatronPersonalData(sampleUser.getId());
        assertNotNull(export);
        assertEquals("Alan Turing", export.getName());
        assertEquals("alan.turing@example.edu", export.getEmail());
        assertEquals("+91-9876543210", export.getPhone());
        assertEquals("MEM-ALAN-01", export.getMemberCode());
        assertEquals(1, export.getLoans().size());
        assertEquals(1, export.getFines().size());
        assertEquals(1, export.getPayments().size());

        // Check audit log
        List<AuditLog> logs = auditLogService.getRecentLogs(10);
        assertTrue(logs.stream().anyMatch(l -> l.getAction().equals("EXPORT_PERSONAL_DATA") && l.getActorId().equals(sampleUser.getId())));
    }

    @Test
    public void testPatronAccountAnonymizationRightToErasure() {
        privacyService.anonymizePatronAccount(sampleUser.getId());

        User anonymized = userRepo.findById(sampleUser.getId()).orElseThrow();
        assertEquals("Anonymized Patron #10", anonymized.getName());
        assertTrue(anonymized.getEmail().startsWith("anonymized-10-"));
        assertTrue(anonymized.getEmail().endsWith("@privacy.local"));
        assertNull(anonymized.getPhone());
        assertNull(anonymized.getMemberCode());
        assertFalse(anonymized.isEnabled());

        // Verify audit log recorded erasure
        List<AuditLog> logs = auditLogService.getRecentLogs(10);
        assertTrue(logs.stream().anyMatch(l -> l.getAction().equals("ANONYMIZE_ACCOUNT_DPDP")));
    }

    @Test
    public void testReadingHistoryPrivacyToggle() {
        assertTrue(sampleUser.isReadingHistoryPrivate());

        privacyService.setReadingHistoryPrivacy(sampleUser.getId(), false);
        User updated = userRepo.findById(sampleUser.getId()).orElseThrow();
        assertFalse(updated.isReadingHistoryPrivate());
    }
}
