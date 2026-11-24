package com.college.library;

import com.college.library.entity.*;
import com.college.library.finance.Fine;
import com.college.library.finance.FineRepository;
import com.college.library.finance.FineStatus;
import com.college.library.policy.MembershipType;
import com.college.library.policy.MembershipTypeRepository;
import com.college.library.repository.BookCopyRepository;
import com.college.library.repository.BookRepository;
import com.college.library.repository.ReservationRepository;
import com.college.library.repository.UserRepository;
import com.college.library.service.ReservationService;
import com.college.library.service.TransactionService;
import com.college.library.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Phase 2: Real Database Critical Business Rules Tests (PostgreSQL)")
class RealDatabaseBusinessRulesTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_athenaeum_rules")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }

    @Autowired(required = false)
    private TransactionService transactionService;

    @Autowired(required = false)
    private ReservationService reservationService;

    @Autowired(required = false)
    private BookRepository bookRepository;

    @Autowired(required = false)
    private BookCopyRepository bookCopyRepository;

    @Autowired(required = false)
    private UserRepository userRepository;

    @Autowired(required = false)
    private ReservationRepository reservationRepository;

    @Autowired(required = false)
    private FineRepository fineRepository;

    @Autowired(required = false)
    private MembershipTypeRepository membershipTypeRepository;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId("campus-main");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Real DB: FIFO Reservation queue correctly fulfills on book return")
    void testRealDbFifoReservationFulfillmentOnReturn() {
        if (postgres == null || !postgres.isRunning()) {
            System.out.println("[SKIP] Docker unavailable, skipped real DB integration test.");
            return;
        }

        assertNotNull(transactionService);

        // 1. Create title and copy in real database
        Book book = new Book();
        book.setTitle("Test Publication Volume One");
        book.setIsbn("9780001112223");
        book.setCategory("Computer Science");
        book.setTenantId("campus-main");
        Book savedBook = bookRepository.save(book);

        BookCopy copy = new BookCopy(savedBook, "BARCODE-DB-FIFO-01", "Rack DB-1", CopyCondition.GOOD);
        copy.setTenantId("campus-main");
        copy.setStatus(CopyStatus.AVAILABLE);
        BookCopy savedCopy = bookCopyRepository.save(copy);

        // 2. Create users
        User borrower = new User("Borrower", "borrower@campus.edu", "pass", Role.MEMBER);
        borrower.setTenantId("campus-main");
        User savedBorrower = userRepository.save(borrower);

        User reserver1 = new User("First Reserver", "res1@campus.edu", "pass", Role.MEMBER);
        reserver1.setTenantId("campus-main");
        User savedReserver1 = userRepository.save(reserver1);

        User reserver2 = new User("Second Reserver", "res2@campus.edu", "pass", Role.MEMBER);
        reserver2.setTenantId("campus-main");
        User savedReserver2 = userRepository.save(reserver2);

        // 3. Issue copy to borrower
        Transaction loan = transactionService.issueCopy(savedCopy, savedBorrower, null);
        assertNotNull(loan.getId());

        // 4. Reserve title for reserver1 then reserver2
        Reservation r1 = reservationService.reserve(savedBook, savedReserver1);
        Reservation r2 = reservationService.reserve(savedBook, savedReserver2);

        assertEquals(ReservationStatus.PENDING, r1.getStatus());
        assertEquals(ReservationStatus.PENDING, r2.getStatus());

        // 5. Return book -> reserver1 should be fulfilled, reserver2 remains pending
        transactionService.returnBook(loan.getId());

        Reservation updatedR1 = reservationRepository.findById(r1.getId()).orElseThrow();
        Reservation updatedR2 = reservationRepository.findById(r2.getId()).orElseThrow();

        assertEquals(ReservationStatus.FULFILLED, updatedR1.getStatus(), "Longest-waiting patron must be fulfilled (FIFO)");
        assertEquals(ReservationStatus.PENDING, updatedR2.getStatus(), "Second patron in queue must remain pending");
    }

    @Test
    @DisplayName("Real DB: Overdue return calculates fine and accrues record in ledger")
    void testRealDbOverdueFineAccrual() {
        if (postgres == null || !postgres.isRunning()) {
            System.out.println("[SKIP] Docker unavailable, skipped real DB integration test.");
            return;
        }

        assertNotNull(transactionService);

        Book book = new Book();
        book.setTitle("Test Publication Volume Two");
        book.setIsbn("9780004445556");
        book.setCategory("Computer Science");
        book.setTenantId("campus-main");
        Book savedBook = bookRepository.save(book);

        BookCopy copy = new BookCopy(savedBook, "BARCODE-DB-FINE-01", "Rack CD-1", CopyCondition.GOOD);
        copy.setTenantId("campus-main");
        copy.setStatus(CopyStatus.AVAILABLE);
        BookCopy savedCopy = bookCopyRepository.save(copy);

        User patron = new User("Late Patron", "late@campus.edu", "pass", Role.MEMBER);
        patron.setTenantId("campus-main");
        User savedPatron = userRepository.save(patron);

        Transaction loan = transactionService.issueCopy(savedCopy, savedPatron, null);
        // Simulate overdue by 5 days (e.g. fine rate ₹5/day = ₹25.00)
        loan.setDueDate(LocalDate.now().minusDays(5));

        Transaction returned = transactionService.returnBook(loan.getId());
        assertTrue(returned.getFineAmount() > 0, "Overdue return must assess a positive fine");

        List<Fine> fines = fineRepository.findByTenantIdAndUserOrderByCreatedAtDesc("campus-main", savedPatron);
        assertFalse(fines.isEmpty(), "Fine record must be persisted in real database fines ledger");
        assertEquals(FineStatus.ACCRUED, fines.get(0).getStatus());
    }
}
