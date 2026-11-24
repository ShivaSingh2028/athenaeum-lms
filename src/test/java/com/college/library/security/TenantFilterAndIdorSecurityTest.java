package com.college.library.security;

import com.college.library.catalog.CatalogApiController;
import com.college.library.controller.NotificationController;
import com.college.library.controller.PatronApiController;
import com.college.library.controller.ReservationController;
import com.college.library.controller.TransactionController;
import com.college.library.entity.*;
import com.college.library.fake.*;
import com.college.library.finance.FineService;
import com.college.library.policy.MembershipType;
import com.college.library.policy.PolicyService;
import com.college.library.privacy.PrivacyService;
import com.college.library.service.*;
import com.college.library.tenant.TenantContext;
import com.college.library.tenant.TenantFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Phase 1: Tenant Spoofing & IDOR Controller Security Tests")
class TenantFilterAndIdorSecurityTest {

    private FakeUserRepository userRepository;
    private FakeReservationRepository reservationRepository;
    private FakeTransactionRepository transactionRepository;
    private FakeNotificationRepository notificationRepository;
    private FakeFineRepository fineRepository;
    private FakePaymentRepository paymentRepository;
    private FakeAuditLogRepository auditLogRepository;
    private FakeBookRepository bookRepository;
    private FakeBookCopyRepository bookCopyRepository;

    private UserService userService;
    private ReservationService reservationService;
    private TransactionService transactionService;
    private NotificationService notificationService;
    private PrivacyService privacyService;

    private User memberTenantA;
    private User member2TenantA;
    private User memberTenantB;
    private User staffTenantA;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        reservationRepository = new FakeReservationRepository();
        transactionRepository = new FakeTransactionRepository();
        notificationRepository = new FakeNotificationRepository();
        fineRepository = new FakeFineRepository();
        paymentRepository = new FakePaymentRepository();
        auditLogRepository = new FakeAuditLogRepository();
        bookRepository = new FakeBookRepository();
        bookCopyRepository = new FakeBookCopyRepository();

        userService = new UserService(userRepository, new BCryptPasswordEncoder());
        notificationService = new NotificationService(notificationRepository);
        reservationService = new ReservationService(reservationRepository, notificationService);

        BookCopyService bookCopyService = new BookCopyService(bookCopyRepository);
        PolicyService policyService = new PolicyService(new FakeMembershipTypeRepository());
        FineService fineService = new FineService(fineRepository, paymentRepository);
        transactionService = new TransactionService(transactionRepository, bookCopyService, reservationService,
                notificationService, policyService, fineService);

        privacyService = new PrivacyService(userRepository, transactionRepository, fineRepository,
                paymentRepository, new com.college.library.audit.AuditLogService(auditLogRepository),
                new BCryptPasswordEncoder());

        // Seed users across tenants
        memberTenantA = new User("Alice", "alice@oxford.edu", "pass123", Role.MEMBER);
        memberTenantA.setId(101L);
        memberTenantA.setTenantId("oxford");
        userRepository.save(memberTenantA);

        member2TenantA = new User("Bob", "bob@oxford.edu", "pass123", Role.MEMBER);
        member2TenantA.setId(102L);
        member2TenantA.setTenantId("oxford");
        userRepository.save(member2TenantA);

        memberTenantB = new User("Charlie", "charlie@cambridge.edu", "pass123", Role.MEMBER);
        memberTenantB.setId(201L);
        memberTenantB.setTenantId("cambridge");
        userRepository.save(memberTenantB);

        staffTenantA = new User("Librarian Dan", "dan@oxford.edu", "pass123", Role.LIBRARIAN);
        staffTenantA.setId(109L);
        staffTenantA.setTenantId("oxford");
        userRepository.save(staffTenantA);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void authenticateAs(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContext securityContext = new SecurityContextImpl(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    private MockHttpSession createAuthenticatedSession(User user) {
        MockHttpSession session = new MockHttpSession();
        CustomUserDetails userDetails = new CustomUserDetails(user,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContext securityContext = new SecurityContextImpl(auth);
        session.setAttribute("SPRING_SECURITY_CONTEXT", securityContext);
        return session;
    }

    @Test
    @DisplayName("TenantFilter: Unauthenticated request in prod profile ignores X-Tenant-ID spoofing")
    void testTenantFilterIgnoresSpoofedHeaderInProd() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        TenantFilter filter = new TenantFilter(prodEnv);
        org.springframework.mock.web.MockHttpServletRequest request =
                new org.springframework.mock.web.MockHttpServletRequest();
        request.setServerName("athenaeum.edu");
        request.addHeader("X-Tenant-ID", "malicious_tenant");

        String resolved = filter.resolveTenantId(request);
        assertEquals(TenantContext.DEFAULT_TENANT_ID, resolved,
                "In prod profile, unauthenticated X-Tenant-ID header must be completely ignored");
    }

    @Test
    @DisplayName("TenantFilter: Subdomain on base domain correctly resolves tenant")
    void testTenantFilterSubdomainResolution() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        TenantFilter filter = new TenantFilter(env);
        org.springframework.mock.web.MockHttpServletRequest request =
                new org.springframework.mock.web.MockHttpServletRequest();
        request.setServerName("oxford.athenaeum.edu");

        String resolved = filter.resolveTenantId(request);
        assertEquals("oxford", resolved);
    }

    @Test
    @DisplayName("TenantFilter: Authenticated user cannot switch tenant via spoofed header")
    void testAuthenticatedUserCannotSwitchTenantViaHeader() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("dev"); // Even in dev!

        TenantFilter filter = new TenantFilter(env);
        org.springframework.mock.web.MockHttpServletRequest request =
                new org.springframework.mock.web.MockHttpServletRequest();
        request.setServerName("athenaeum.edu");
        request.addHeader("X-Tenant-ID", "cambridge");

        // User is authenticated as oxford member
        MockHttpSession session = createAuthenticatedSession(memberTenantA);
        request.setSession(session);

        String resolved = filter.resolveTenantId(request);
        assertEquals("oxford", resolved,
                "Authenticated user tenant must always override headers and prevent tenant hopping");
    }

    @Test
    @DisplayName("IDOR Negative Test: Member A cannot cancel Member B's reservation")
    void testIdorMemberCannotCancelOtherMembersReservation() {
        TenantContext.setTenantId("oxford");
        authenticateAs(memberTenantA);

        Reservation bobReservation = new Reservation();
        bobReservation.setId(501L);
        bobReservation.setTenantId("oxford");
        bobReservation.setUser(member2TenantA);
        bobReservation.setStatus(ReservationStatus.PENDING);
        reservationRepository.save(bobReservation);

        ReservationController controller = new ReservationController(reservationService,
                new BookService(bookRepository, new BookCopyService(bookCopyRepository)), userService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/reservations/501/cancel"));
        });
    }

    @Test
    @DisplayName("IDOR Negative Test: Tenant A cannot cancel Tenant B's reservation")
    void testIdorCrossTenantReservationCancelForbidden() {
        TenantContext.setTenantId("oxford");
        authenticateAs(staffTenantA);

        // Reservation in cambridge
        Reservation cambridgeReservation = new Reservation();
        cambridgeReservation.setId(601L);
        cambridgeReservation.setTenantId("cambridge");
        cambridgeReservation.setUser(memberTenantB);
        cambridgeReservation.setStatus(ReservationStatus.PENDING);
        reservationRepository.save(cambridgeReservation);

        ReservationController controller = new ReservationController(reservationService,
                new BookService(bookRepository, new BookCopyService(bookCopyRepository)), userService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/reservations/601/cancel"));
        });
    }

    @Test
    @DisplayName("IDOR Negative Test: Member A cannot self-return Member B's loan")
    void testIdorMemberCannotReturnOtherMembersLoan() {
        TenantContext.setTenantId("oxford");
        authenticateAs(memberTenantA);

        Transaction bobLoan = new Transaction();
        bobLoan.setId(701L);
        bobLoan.setTenantId("oxford");
        bobLoan.setUser(member2TenantA);
        bobLoan.setStatus(TransactionStatus.ISSUED);
        bobLoan.setIssueDate(LocalDate.now());
        bobLoan.setDueDate(LocalDate.now().plusDays(14));
        transactionRepository.save(bobLoan);

        TransactionController controller = new TransactionController(transactionService,
                new BookService(bookRepository, new BookCopyService(bookCopyRepository)), userService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        // Self-return by Alice on Bob's loan redirects with flash error attribute
        assertDoesNotThrow(() -> {
            mockMvc.perform(post("/transactions/701/self-return"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("error"));
        });

        // Ensure status is still ISSUED (unreturned)
        assertEquals(TransactionStatus.ISSUED, transactionRepository.findById(701L).get().getStatus());
    }

    @Test
    @DisplayName("IDOR Negative Test: Staff A cannot return Tenant B's loan")
    void testIdorStaffCannotReturnCrossTenantLoan() {
        TenantContext.setTenantId("oxford");
        authenticateAs(staffTenantA);

        Transaction cambridgeLoan = new Transaction();
        cambridgeLoan.setId(801L);
        cambridgeLoan.setTenantId("cambridge");
        cambridgeLoan.setUser(memberTenantB);
        cambridgeLoan.setStatus(TransactionStatus.ISSUED);
        cambridgeLoan.setIssueDate(LocalDate.now());
        cambridgeLoan.setDueDate(LocalDate.now().plusDays(14));
        transactionRepository.save(cambridgeLoan);

        TransactionController controller = new TransactionController(transactionService,
                new BookService(bookRepository, new BookCopyService(bookCopyRepository)), userService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        assertDoesNotThrow(() -> {
            mockMvc.perform(post("/transactions/801/return"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(flash().attributeExists("error"));
        });

        assertEquals(TransactionStatus.ISSUED, transactionRepository.findById(801L).get().getStatus());
    }

    @Test
    @DisplayName("IDOR Negative Test: Member A cannot mark Member B's notification as read")
    void testIdorMemberCannotMarkOtherNotificationRead() {
        TenantContext.setTenantId("oxford");
        authenticateAs(memberTenantA);

        Notification bobNotification = new Notification(member2TenantA, "Notice", "Private notice", "CIRCULATION");
        bobNotification.setId(901L);
        bobNotification.setTenantId("oxford");
        bobNotification.setRead(false);
        notificationRepository.save(bobNotification);

        NotificationController controller = new NotificationController(notificationService, userService);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

        assertThrows(Exception.class, () -> {
            mockMvc.perform(post("/notifications/901/read"));
        });

        assertFalse(notificationRepository.findById(901L).get().isRead());
    }

    @Test
    @DisplayName("IDOR Negative Test: Tenant A cannot export Tenant B patron's personal data")
    void testIdorCrossTenantDataExportForbidden() {
        TenantContext.setTenantId("oxford");
        authenticateAs(memberTenantA);

        // Attempt to export memberTenantB (cambridge) while in oxford context
        assertThrows(AccessDeniedException.class, () -> {
            privacyService.exportPatronPersonalData(memberTenantB.getId(), memberTenantA);
        });
    }
}
