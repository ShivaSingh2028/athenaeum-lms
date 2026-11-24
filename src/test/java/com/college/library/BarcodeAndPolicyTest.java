package com.college.library;

import com.college.library.barcode.BarcodeService;
import com.college.library.entity.*;
import com.college.library.policy.MembershipType;
import com.college.library.policy.MembershipTypeRepository;
import com.college.library.policy.PolicyService;
import com.college.library.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 2: Barcode & Policy Engine Unit Tests")
class BarcodeAndPolicyTest {

    private BarcodeService barcodeService;
    private PolicyService policyService;
    private FakeMembershipTypeRepository policyRepository;

    @BeforeEach
    void setUp() {
        barcodeService = new BarcodeService();
        policyRepository = new FakeMembershipTypeRepository();
        policyService = new PolicyService(policyRepository);
        TenantContext.setTenantId("campus-test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Verify Code128 Barcode Generation & PNG Signature")
    void testCode128Generation() throws Exception {
        byte[] barcode = barcodeService.generateCode128("BC-TEST-101", 200, 60);
        assertNotNull(barcode);
        assertTrue(barcode.length > 50, "Barcode image byte array must be generated");
        // Verify PNG magic bytes: 0x89 0x50 0x4E 0x47
        assertEquals((byte) 0x89, barcode[0]);
        assertEquals((byte) 0x50, barcode[1]);
        assertEquals((byte) 0x4E, barcode[2]);
        assertEquals((byte) 0x47, barcode[3]);
    }

    @Test
    @DisplayName("Verify QR Code Generation & PNG Signature")
    void testQrCodeGeneration() throws Exception {
        byte[] qr = barcodeService.generateQrCode("patron@college.edu", 150, 150);
        assertNotNull(qr);
        assertTrue(qr.length > 50, "QR code image byte array must be generated");
        assertEquals((byte) 0x89, qr[0]);
        assertEquals((byte) 0x50, qr[1]);
        assertEquals((byte) 0x4E, qr[2]);
        assertEquals((byte) 0x47, qr[3]);
    }

    @Test
    @DisplayName("Verify Policy Engine Defaults & Due Date Calculation")
    void testPolicyDueDateCalculation() {
        policyService.initDefaultPoliciesIfMissing("campus-test");

        User student = new User();
        student.setTenantId("campus-test");
        student.setRole(Role.MEMBER);

        LocalDate dueDate = policyService.calculateDueDate(student);
        assertEquals(LocalDate.now().plusDays(14), dueDate, "Default UG loan duration is 14 days");
    }

    @Test
    @DisplayName("Verify Policy Engine Quota Borrowing Limits")
    void testBorrowingQuotaLimit() {
        policyService.initDefaultPoliciesIfMissing("campus-test");

        User student = new User();
        student.setTenantId("campus-test");
        student.setRole(Role.MEMBER);
        student.setEnabled(true);

        BookCopy copy = new BookCopy();
        copy.setBarcode("BC-101");
        copy.setStatus(CopyStatus.AVAILABLE);

        // Within quota (2 active loans, max is 3) -> OK
        assertDoesNotThrow(() -> policyService.validateCanBorrow(student, copy, 2));

        // At quota (3 active loans, max is 3) -> Throws IllegalStateException
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            policyService.validateCanBorrow(student, copy, 3);
        });
        assertTrue(ex.getMessage().contains("Borrowing quota exceeded"));
    }

    @Test
    @DisplayName("Verify Fine Calculation with Grace Period and Fine Cap")
    void testFineCalculationRules() {
        policyService.initDefaultPoliciesIfMissing("campus-test");

        User student = new User();
        student.setTenantId("campus-test");
        student.setRole(Role.MEMBER);

        Transaction tx = new Transaction();
        tx.setUser(student);
        tx.setDueDate(LocalDate.now().minusDays(10)); // 10 days late

        // Fine: 10 days * ₹5/day = ₹50
        BigDecimal fine = policyService.calculateFine(tx, LocalDate.now());
        assertEquals(new BigDecimal("50.00"), fine);

        // Fine cap check: 200 days late * ₹5 = ₹1000, capped at ₹500
        tx.setDueDate(LocalDate.now().minusDays(200));
        BigDecimal cappedFine = policyService.calculateFine(tx, LocalDate.now());
        assertEquals(new BigDecimal("500.00"), cappedFine);
    }

    // Lightweight in-memory fake repository for policy tiers
    static class FakeMembershipTypeRepository implements MembershipTypeRepository {
        private final Map<Long, MembershipType> db = new HashMap<>();
        private long idGen = 1;

        @Override
        public Optional<MembershipType> findByTenantIdAndCode(String tenantId, String code) {
            return db.values().stream()
                    .filter(p -> Objects.equals(p.getTenantId(), tenantId) && Objects.equals(p.getCode(), code))
                    .findFirst();
        }

        @Override
        public List<MembershipType> findByTenantId(String tenantId) {
            return db.values().stream()
                    .filter(p -> Objects.equals(p.getTenantId(), tenantId))
                    .toList();
        }

        @Override
        public List<MembershipType> findByTenantIdAndIsActiveTrue(String tenantId) {
            return db.values().stream()
                    .filter(p -> Objects.equals(p.getTenantId(), tenantId) && p.isActive())
                    .toList();
        }

        @Override
        public boolean existsByTenantIdAndCode(String tenantId, String code) {
            return findByTenantIdAndCode(tenantId, code).isPresent();
        }

        @Override
        public <S extends MembershipType> S save(S entity) {
            if (entity.getId() == null) entity.setId(idGen++);
            db.put(entity.getId(), entity);
            return entity;
        }

        @Override public Optional<MembershipType> findById(Long id) { return Optional.ofNullable(db.get(id)); }
        @Override public List<MembershipType> findAll() { return new ArrayList<>(db.values()); }
        @Override public long count() { return db.size(); }
        @Override public void delete(MembershipType entity) { if (entity != null) db.remove(entity.getId()); }
        @Override public void deleteById(Long id) { db.remove(id); }
        @Override public boolean existsById(Long id) { return db.containsKey(id); }
        @Override public void flush() {}
        @Override public <S extends MembershipType> S saveAndFlush(S entity) { return save(entity); }
        @Override public <S extends MembershipType> List<S> saveAllAndFlush(Iterable<S> entities) { return saveAll(entities); }
        @Override public void deleteAllInBatch(Iterable<MembershipType> entities) { db.clear(); }
        @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { ids.forEach(db::remove); }
        @Override public void deleteAllInBatch() { db.clear(); }
        @Override public MembershipType getOne(Long id) { return db.get(id); }
        @Override public MembershipType getById(Long id) { return db.get(id); }
        @Override public MembershipType getReferenceById(Long id) { return db.get(id); }
        @Override public <S extends MembershipType> List<S> saveAll(Iterable<S> entities) { List<S> list = new ArrayList<>(); entities.forEach(e -> list.add(save(e))); return list; }
        @Override public List<MembershipType> findAllById(Iterable<Long> ids) { List<MembershipType> list = new ArrayList<>(); ids.forEach(id -> findById(id).ifPresent(list::add)); return list; }
        @Override public void deleteAllById(Iterable<? extends Long> ids) { ids.forEach(db::remove); }
        @Override public void deleteAll(Iterable<? extends MembershipType> entities) { entities.forEach(this::delete); }
        @Override public void deleteAll() { db.clear(); }
        @Override public List<MembershipType> findAll(org.springframework.data.domain.Sort sort) { return findAll(); }
        @Override public org.springframework.data.domain.Page<MembershipType> findAll(org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends MembershipType> Optional<S> findOne(org.springframework.data.domain.Example<S> example) { return Optional.empty(); }
        @Override public <S extends MembershipType> List<S> findAll(org.springframework.data.domain.Example<S> example) { return Collections.emptyList(); }
        @Override public <S extends MembershipType> List<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Sort sort) { return Collections.emptyList(); }
        @Override public <S extends MembershipType> org.springframework.data.domain.Page<S> findAll(org.springframework.data.domain.Example<S> example, org.springframework.data.domain.Pageable pageable) { return org.springframework.data.domain.Page.empty(); }
        @Override public <S extends MembershipType> long count(org.springframework.data.domain.Example<S> example) { return 0; }
        @Override public <S extends MembershipType> boolean exists(org.springframework.data.domain.Example<S> example) { return false; }
        @Override public <S extends MembershipType, R> R findBy(org.springframework.data.domain.Example<S> example, java.util.function.Function<org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery<S>, R> queryFunction) { return null; }
    }
}
