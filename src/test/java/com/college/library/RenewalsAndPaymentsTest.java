package com.college.library;

import com.college.library.entity.*;
import com.college.library.fake.FakeBookCopyRepository;
import com.college.library.finance.Fine;
import com.college.library.finance.FineRepository;
import com.college.library.finance.FineService;
import com.college.library.finance.FineStatus;
import com.college.library.payment.*;
import com.college.library.policy.MembershipType;
import com.college.library.policy.MembershipTypeRepository;
import com.college.library.policy.PolicyService;
import com.college.library.repository.TransactionRepository;
import com.college.library.service.BookCopyService;
import com.college.library.service.NotificationService;
import com.college.library.service.ReservationService;
import com.college.library.service.TransactionService;
import com.college.library.tenant.TenantContext;
import com.college.library.tenant.TenantSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 3: Renewals, Blocking Rules & Payments Unit Tests")
class RenewalsAndPaymentsTest {

    private FakeTransactionRepository transactionRepository;
    private FakeBookCopyRepository bookCopyRepository;
    private FakeFineRepository fineRepository;
    private FakePaymentRepository paymentRepository;
    private FakeMembershipTypeRepository membershipTypeRepository;

    private BookCopyService bookCopyService;
    private ReservationService reservationService;
    private NotificationService notificationService;
    private PolicyService policyService;
    private FineService fineService;
    private TransactionService transactionService;
    private PdfReceiptGenerator receiptGenerator;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("campus-test");
        transactionRepository = new FakeTransactionRepository();
        bookCopyRepository = new FakeBookCopyRepository();
        fineRepository = new FakeFineRepository();
        paymentRepository = new FakePaymentRepository();
        membershipTypeRepository = new FakeMembershipTypeRepository();

        bookCopyService = new BookCopyService(bookCopyRepository);
        reservationService = new FakeReservationService();
        notificationService = new FakeNotificationService();
        policyService = new PolicyService(membershipTypeRepository);
        policyService.initDefaultPoliciesIfMissing("campus-test");

        fineService = new FineService(fineRepository, paymentRepository);
        transactionService = new TransactionService(
                transactionRepository,
                bookCopyService,
                reservationService,
                notificationService,
                policyService,
                fineService
        );
        receiptGenerator = new PdfReceiptGenerator();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Verify Renewal Blocked when Holding Reservation Exists")
    void testRenewalBlockedByHoldingReservation() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.MEMBER);

        Book book = new Book();
        book.setId(10L);
        book.setTitle("Domain-Driven Design");

        Transaction tx = new Transaction();
        tx.setId(100L);
        tx.setUser(user);
        tx.setBook(book);
        tx.setStatus(TransactionStatus.ISSUED);
        tx.setRenewalCount(0);
        tx.setDueDate(LocalDate.now().plusDays(2));
        transactionRepository.save(tx);

        // Mark holding reservation as existing
        ((FakeReservationService) reservationService).setHasHold(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            transactionService.renewLoan(100L, user);
        });
        assertTrue(ex.getMessage().contains("Another library member has reserved this title"));
    }

    @Test
    @DisplayName("Verify Renewal Blocked when Unpaid Fines Exceed Threshold")
    void testRenewalBlockedByFineThreshold() {
        User user = new User();
        user.setId(2L);
        user.setRole(Role.MEMBER);

        Book book = new Book();
        book.setId(20L);

        Transaction tx = new Transaction();
        tx.setId(200L);
        tx.setUser(user);
        tx.setBook(book);
        tx.setStatus(TransactionStatus.ISSUED);
        tx.setRenewalCount(0);
        tx.setDueDate(LocalDate.now().plusDays(3));
        transactionRepository.save(tx);

        // Accrue fine exceeding ₹100 threshold
        Fine fine = new Fine(tx, user, new BigDecimal("150.00"));
        fineRepository.save(fine);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            transactionService.renewLoan(200L, user);
        });
        assertTrue(ex.getMessage().contains("exceed the ₹100.00 threshold"));
    }

    @Test
    @DisplayName("Verify Mandatory Reason Required for Staff Fine Waiver")
    void testFineWaiverAuditReasonMandatory() {
        Fine fine = new Fine();
        fine.setId(50L);
        fine.setAmount(new BigDecimal("50.00"));
        fine.setStatus(FineStatus.ACCRUED);
        fineRepository.save(fine);

        User staff = new User();
        staff.setName("Head Librarian");

        // Blank reason must be rejected
        assertThrows(IllegalArgumentException.class, () -> {
            fineService.waiveFine(50L, new BigDecimal("50.00"), "", staff);
        });

        // Valid reason succeeds
        Fine waived = fineService.waiveFine(50L, new BigDecimal("50.00"), "Medical hospitalization waiver #MED-104", staff);
        assertEquals(FineStatus.WAIVED, waived.getStatus());
        assertEquals("Medical hospitalization waiver #MED-104", waived.getWaiverReason());
        assertEquals(staff, waived.getWaivedBy());
    }

    @Test
    @DisplayName("Verify Payment Idempotency Guard")
    void testPaymentIdempotency() {
        String idempotencyKey = "PAY-IDEMP-998877";
        Payment existingPayment = new Payment();
        existingPayment.setId(1L);
        existingPayment.setIdempotencyKey(idempotencyKey);
        existingPayment.setAmount(new BigDecimal("50.00"));
        paymentRepository.save(existingPayment);

        User staff = new User();
        Payment payment = fineService.recordPayment(null, new BigDecimal("50.00"), PaymentMethod.CASH, "REC-1", idempotencyKey, staff);

        assertEquals(1L, payment.getId());
        assertEquals(1, paymentRepository.findAll().size(), "Duplicate payment must not be recorded");
    }

    @Test
    @DisplayName("Verify PDF Receipt Generation & Magic Bytes")
    void testPdfReceiptGeneration() throws Exception {
        User patron = new User();
        patron.setName("Asha Verma");
        patron.setEmail("asha@college.edu");

        Payment payment = new Payment(null, patron, new BigDecimal("45.00"), PaymentMethod.RAZORPAY_UPI, "pay_mock_123");
        payment.setId(88L);

        TenantSettings settings = new TenantSettings("campus-test", "Athenaeum Central Library");

        byte[] pdf = receiptGenerator.generateReceiptPdf(payment, settings);
        assertNotNull(pdf);
        assertTrue(pdf.length > 500, "PDF receipt byte array must be non-empty");

        // Verify PDF Magic Bytes: %PDF-
        assertEquals((byte) 0x25, pdf[0]);
        assertEquals((byte) 0x50, pdf[1]);
        assertEquals((byte) 0x44, pdf[2]);
        assertEquals((byte) 0x46, pdf[3]);
        assertEquals((byte) 0x2D, pdf[4]);
    }

    // --- Lightweight test fakes below ---
    static class FakeTransactionRepository implements TransactionRepository {
        private final Map<Long, Transaction> db = new HashMap<>();
        private long idGen = 1;
        @Override public <S extends Transaction> S save(S e) { if (e.getId() == null) e.setId(idGen++); db.put(e.getId(), e); return e; }
        @Override public Optional<Transaction> findById(Long id) { return Optional.ofNullable(db.get(id)); }
        @Override public List<Transaction> findByTenantIdOrderByIssueDateDesc(String t) { return new ArrayList<>(db.values()); }
        @Override public List<Transaction> findByTenantIdAndUserOrderByIssueDateDesc(String t, User u) { return db.values().stream().filter(x -> Objects.equals(x.getUser().getId(), u.getId())).toList(); }
        @Override public List<Transaction> findByTenantIdAndStatus(String t, TransactionStatus s) { return db.values().stream().filter(x -> x.getStatus() == s).toList(); }
        @Override public List<Transaction> findByTenantIdAndStatusAndDueDateBefore(String t, TransactionStatus s, LocalDate d) { return Collections.emptyList(); }
        @Override public List<Transaction> findByTenantIdAndBookIdAndStatus(String t, Long b, TransactionStatus s) { return Collections.emptyList(); }
        @Override public long countByTenantIdAndStatus(String t, TransactionStatus s) { return findByTenantIdAndStatus(t, s).size(); }
        @Override public List<Transaction> findAll() { return new ArrayList<>(db.values()); }
        @Override public List<Transaction> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
        @Override public <S extends Transaction> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public boolean existsById(Long id) { return db.containsKey(id); }
        @Override public long count() { return db.size(); }
        @Override public void deleteById(Long id) { db.remove(id); }
        @Override public void delete(Transaction entity) { if (entity != null) db.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(db::remove); }
        @Override public void deleteAll(Iterable<? extends Transaction> entities) { entities.forEach(this::delete); }
        @Override public void deleteAll() { db.clear(); }
        @Override public void flush() {}
        @Override public <S extends Transaction> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Transaction> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<Transaction> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public Transaction getOne(Long id) { return db.get(id); }
        @Override public Transaction getById(Long id) { return db.get(id); }
        @Override public Transaction getReferenceById(Long id) { return db.get(id); }
        @Override public <S extends Transaction> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends Transaction> List<S> findAll(org.springframework.data.domain.Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Transaction> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Transaction> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends Transaction> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends Transaction> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends Transaction, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<Transaction> findAll(org.springframework.data.domain.Sort sort) { return findAll(); }
        @Override public org.springframework.data.domain.Page<Transaction> findAll(org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
    }

    static class FakeFineRepository implements FineRepository {
        private final Map<Long, Fine> db = new HashMap<>();
        private long idGen = 1;
        @Override public <S extends Fine> S save(S e) { if (e.getId() == null) e.setId(idGen++); db.put(e.getId(), e); return e; }
        @Override public Optional<Fine> findById(Long id) { return Optional.ofNullable(db.get(id)); }
        @Override public List<Fine> findByTenantIdOrderByCreatedAtDesc(String t) { return new ArrayList<>(db.values()); }
        @Override public List<Fine> findByTenantIdAndUserOrderByCreatedAtDesc(String t, User u) { return db.values().stream().filter(f -> Objects.equals(f.getUser().getId(), u.getId())).toList(); }
        @Override public List<Fine> findByTenantIdAndStatus(String t, FineStatus s) { return db.values().stream().filter(f -> f.getStatus() == s).toList(); }
        @Override public BigDecimal sumUnpaidFinesForUser(String t, User u) {
            return db.values().stream()
                    .filter(f -> Objects.equals(f.getUser().getId(), u.getId()) && (f.getStatus() == FineStatus.ACCRUED || f.getStatus() == FineStatus.PARTIALLY_PAID))
                    .map(Fine::getOutstandingAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        @Override public BigDecimal sumTotalPaidFines(String t) { return BigDecimal.ZERO; }
        @Override public BigDecimal sumTotalOutstandingFines(String t) { return BigDecimal.ZERO; }
        @Override public List<Fine> findAll() { return new ArrayList<>(db.values()); }
        @Override public List<Fine> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
        @Override public <S extends Fine> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public boolean existsById(Long id) { return db.containsKey(id); }
        @Override public long count() { return db.size(); }
        @Override public void deleteById(Long id) { db.remove(id); }
        @Override public void delete(Fine entity) { if (entity != null) db.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(db::remove); }
        @Override public void deleteAll(Iterable<? extends Fine> entities) { entities.forEach(this::delete); }
        @Override public void deleteAll() { db.clear(); }
        @Override public void flush() {}
        @Override public <S extends Fine> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Fine> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<Fine> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public Fine getOne(Long id) { return db.get(id); }
        @Override public Fine getById(Long id) { return db.get(id); }
        @Override public Fine getReferenceById(Long id) { return db.get(id); }
        @Override public <S extends Fine> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends Fine> List<S> findAll(org.springframework.data.domain.Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Fine> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Fine> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends Fine> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends Fine> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends Fine, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<Fine> findAll(org.springframework.data.domain.Sort sort) { return findAll(); }
        @Override public org.springframework.data.domain.Page<Fine> findAll(org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
    }

    static class FakePaymentRepository implements PaymentRepository {
        private final Map<Long, Payment> db = new HashMap<>();
        private long idGen = 1;
        @Override public <S extends Payment> S save(S e) { if (e.getId() == null) e.setId(idGen++); db.put(e.getId(), e); return e; }
        @Override public Optional<Payment> findById(Long id) { return Optional.ofNullable(db.get(id)); }
        @Override public Optional<Payment> findByIdempotencyKey(String key) { return db.values().stream().filter(p -> Objects.equals(p.getIdempotencyKey(), key)).findFirst(); }
        @Override public boolean existsByIdempotencyKey(String key) { return findByIdempotencyKey(key).isPresent(); }
        @Override public Optional<Payment> findByTransactionReference(String ref) { return db.values().stream().filter(p -> Objects.equals(p.getTransactionReference(), ref)).findFirst(); }
        @Override public List<Payment> findByTenantIdOrderByPaymentDateDesc(String t) { return new ArrayList<>(db.values()); }
        @Override public List<Payment> findByTenantIdAndUserOrderByPaymentDateDesc(String t, User u) { return db.values().stream().filter(p -> Objects.equals(p.getUser().getId(), u.getId())).toList(); }
        @Override public List<Payment> findAll() { return new ArrayList<>(db.values()); }
        @Override public List<Payment> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
        @Override public <S extends Payment> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public boolean existsById(Long id) { return db.containsKey(id); }
        @Override public long count() { return db.size(); }
        @Override public void deleteById(Long id) { db.remove(id); }
        @Override public void delete(Payment entity) { if (entity != null) db.remove(entity.getId()); }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(db::remove); }
        @Override public void deleteAll(Iterable<? extends Payment> entities) { entities.forEach(this::delete); }
        @Override public void deleteAll() { db.clear(); }
        @Override public void flush() {}
        @Override public <S extends Payment> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends Payment> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<Payment> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public Payment getOne(Long id) { return db.get(id); }
        @Override public Payment getById(Long id) { return db.get(id); }
        @Override public Payment getReferenceById(Long id) { return db.get(id); }
        @Override public <S extends Payment> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends Payment> List<S> findAll(org.springframework.data.domain.Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends Payment> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override public <S extends Payment> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends Payment> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends Payment> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends Payment, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<Payment> findAll(org.springframework.data.domain.Sort sort) { return findAll(); }
        @Override public org.springframework.data.domain.Page<Payment> findAll(org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
    }

    static class FakeReservationService extends ReservationService {
        private boolean hasHold = false;
        public FakeReservationService() { super(null, null); }
        public void setHasHold(boolean hasHold) { this.hasHold = hasHold; }
        @Override public boolean hasPendingReservation(Book book) { return hasHold; }
    }

    static class FakeNotificationService extends NotificationService {
        public FakeNotificationService() { super(null); }
        @Override public void notify(User user, String msg) {}
    }

    static class FakeMembershipTypeRepository implements MembershipTypeRepository {
        private final Map<String, MembershipType> db = new HashMap<>();
        private long idGen = 1;
        @Override public Optional<MembershipType> findByTenantIdAndCode(String t, String c) { return Optional.ofNullable(db.get(t + "_" + c)); }
        @Override public List<MembershipType> findByTenantId(String t) { return new ArrayList<>(db.values()); }
        @Override public List<MembershipType> findByTenantIdAndIsActiveTrue(String t) { return new ArrayList<>(db.values()); }
        @Override public boolean existsByTenantIdAndCode(String t, String c) { return db.containsKey(t + "_" + c); }
        @Override public <S extends MembershipType> S save(S e) { if (e.getId() == null) e.setId(idGen++); db.put(e.getTenantId() + "_" + e.getCode(), e); return e; }
        @Override public Optional<MembershipType> findById(Long id) { return db.values().stream().filter(x -> Objects.equals(x.getId(), id)).findFirst(); }
        @Override public boolean existsById(Long id) { return findById(id).isPresent(); }
        @Override public List<MembershipType> findAll() { return new ArrayList<>(db.values()); }
        @Override public List<MembershipType> findAllById(Iterable<Long> ids) { return Collections.emptyList(); }
        @Override public <S extends MembershipType> List<S> saveAll(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public long count() { return db.size(); }
        @Override public void deleteById(Long id) {}
        @Override public void delete(MembershipType entity) {}
        @Override public void deleteAllById(Iterable<? extends Long> ids) {}
        @Override public void deleteAll(Iterable<? extends MembershipType> entities) {}
        @Override public void deleteAll() { db.clear(); }
        @Override public void flush() {}
        @Override public <S extends MembershipType> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends MembershipType> List<S> saveAllAndFlush(Iterable<S> entities) { return Collections.emptyList(); }
        @Override public void deleteAllInBatch(Iterable<MembershipType> entities) {}
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) {}
        @Override public void deleteAllInBatch() {}
        @Override public MembershipType getOne(Long id) { return null; }
        @Override public MembershipType getById(Long id) { return null; }
        @Override public MembershipType getReferenceById(Long id) { return null; }
        @Override public <S extends MembershipType> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends MembershipType> List<S> findAll(org.springframework.data.domain.Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends MembershipType> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override public <S extends MembershipType> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends MembershipType> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends MembershipType> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends MembershipType, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
        @Override public List<MembershipType> findAll(org.springframework.data.domain.Sort sort) { return findAll(); }
        @Override public org.springframework.data.domain.Page<MembershipType> findAll(org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
    }
}
