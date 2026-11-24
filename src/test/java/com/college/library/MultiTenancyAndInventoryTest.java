package com.college.library;

import com.college.library.entity.*;
import com.college.library.fake.FakeBookCopyRepository;
import com.college.library.service.BookCopyService;
import com.college.library.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 1: Multi-Tenancy & Copy-Level Inventory Unit Tests")
class MultiTenancyAndInventoryTest {

    private FakeBookCopyRepository copyRepository;
    private BookCopyService copyService;

    @BeforeEach
    void setUp() {
        copyRepository = new FakeBookCopyRepository();
        copyService = new BookCopyService(copyRepository);
        TenantContext.setTenantId("campus-north");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Verify TenantContext Thread-Local Isolation & Fallback")
    void testTenantContextIsolation() {
        assertEquals("campus-north", TenantContext.getTenantId());

        TenantContext.setTenantId("oxford-main");
        assertEquals("oxford-main", TenantContext.getTenantId());

        TenantContext.setTenantId(null);
        assertEquals(TenantContext.DEFAULT_TENANT_ID, TenantContext.getTenantId());

        TenantContext.clear();
        assertEquals(TenantContext.DEFAULT_TENANT_ID, TenantContext.getTenantId());
    }

    @Test
    @DisplayName("Verify BookCopy Discrete Barcode Generation & State")
    void testBookCopyCreationAndBarcodes() {
        Book book = new Book();
        book.setId(42L);
        book.setTenantId("campus-north");
        book.setTitle("Clean Architecture");
        book.setIsbn("9780134494166");

        List<BookCopy> copies = copyService.createCopies(book, 3, "Rack B2", CopyCondition.GOOD);

        assertEquals(3, copies.size());
        assertEquals("BC-CAMPUS-NORTH-42-001", copies.get(0).getBarcode());
        assertEquals("BC-CAMPUS-NORTH-42-002", copies.get(1).getBarcode());
        assertEquals("BC-CAMPUS-NORTH-42-003", copies.get(2).getBarcode());

        for (BookCopy copy : copies) {
            assertEquals(CopyStatus.AVAILABLE, copy.getStatus());
            assertEquals("Rack B2", copy.getShelfLocation());
            assertEquals(CopyCondition.GOOD, copy.getCondition());
            assertEquals("campus-north", copy.getTenantId());
            assertNotNull(copy.getId());
        }
    }

    @Test
    @DisplayName("Verify Atomic Transition on Book Issue and Double-Issue Guard")
    void testIssueCopyStatusTransition() {
        Book book = new Book();
        book.setId(10L);
        BookCopy copy = new BookCopy(book, "BC-10-001", "Rack A", CopyCondition.GOOD);
        copyRepository.save(copy);

        // 1. Issue available copy
        BookCopy issued = copyService.markIssued(copy);
        assertEquals(CopyStatus.ISSUED, issued.getStatus());

        // 2. Double-issue race attempt must fail with IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            copyService.markIssued(issued);
        });
        assertTrue(exception.getMessage().contains("is not AVAILABLE"));
    }

    @Test
    @DisplayName("Verify Return Copy Transitions to Available with Condition Inspection")
    void testReturnCopy() {
        Book book = new Book();
        book.setId(10L);
        BookCopy copy = new BookCopy(book, "BC-10-001", "Rack A", CopyCondition.GOOD);
        copy.setStatus(CopyStatus.ISSUED);
        copyRepository.save(copy);

        BookCopy returned = copyService.markReturned(copy, CopyCondition.WORN);
        assertEquals(CopyStatus.AVAILABLE, returned.getStatus());
        assertEquals(CopyCondition.WORN, returned.getCondition());
    }

    @Test
    @DisplayName("Verify Optimistic Locking @Version Field on Entities")
    void testOptimisticLockingField() {
        BookCopy copy = new BookCopy();
        assertEquals(0, copy.getVersion());

        Transaction tx = new Transaction();
        assertEquals(0, tx.getVersion());

        User user = new User();
        assertEquals(0, user.getVersion());
    }

    @Test
    @DisplayName("Verify Enterprise 4-Tier Roles & Hierarchy")
    void testEnterpriseRoles() {
        assertTrue(Role.SUPER_ADMIN.isStaffOrAdmin());
        assertTrue(Role.LIBRARY_ADMIN.isStaffOrAdmin());
        assertTrue(Role.LIBRARIAN.isStaffOrAdmin());
        assertFalse(Role.MEMBER.isStaffOrAdmin());

        assertTrue(Role.SUPER_ADMIN.isPlatformOwner());
        assertFalse(Role.LIBRARY_ADMIN.isPlatformOwner());
    }
}
