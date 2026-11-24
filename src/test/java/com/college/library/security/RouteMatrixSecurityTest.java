package com.college.library.security;

import com.college.library.config.CustomUserDetailsService;
import com.college.library.config.SecurityConfig;
import com.college.library.controller.AdminController;
import com.college.library.controller.BookController;
import com.college.library.controller.CirculationDeskController;
import com.college.library.controller.TransactionController;
import com.college.library.entity.Role;
import com.college.library.entity.User;
import com.college.library.repository.UserRepository;
import com.college.library.service.*;
import com.college.library.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AdminController.class,
        BookController.class,
        CirculationDeskController.class,
        TransactionController.class
})
@Import({SecurityConfig.class, CustomUserDetailsService.class, LoginSecurityService.class})
@ActiveProfiles("test")
@DisplayName("Phase 2: Route Matrix Security Role Verification Tests")
class RouteMatrixSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserService userService;

    @MockBean
    private BookService bookService;

    @MockBean
    private BookCopyService bookCopyService;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private ReservationService reservationService;

    @MockBean
    private ReportService reportService;

    @BeforeEach
    void setup() {
        TenantContext.setTenantId("campus-main");
        User dummyAdmin = new User("Admin", "admin@campus.edu", "pass", Role.ADMIN);
        dummyAdmin.setId(1L);
        when(userRepository.findByTenantIdAndEmail(anyString(), anyString())).thenReturn(Optional.of(dummyAdmin));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // 1. ANONYMOUS ACCESS TESTS
    @Test
    @DisplayName("Anonymous: Public login page accessible")
    void testAnonymousCanAccessLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Anonymous: Admin routes redirect to login (302)")
    void testAnonymousBlockedFromAdmin() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Anonymous: Member transactions redirect to login (302)")
    void testAnonymousBlockedFromMemberTransactions() throws Exception {
        mockMvc.perform(get("/transactions/my"))
                .andExpect(status().is3xxRedirection());
    }

    // 2. MEMBER ACCESS TESTS
    @Test
    @WithMockUser(username = "member@campus.edu", roles = {"MEMBER"})
    @DisplayName("Member: Can view own transactions")
    void testMemberCanAccessMyTransactions() throws Exception {
        mockMvc.perform(get("/transactions/my"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "member@campus.edu", roles = {"MEMBER"})
    @DisplayName("Member: Forbidden from admin dashboard (403)")
    void testMemberForbiddenFromAdmin() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "member@campus.edu", roles = {"MEMBER"})
    @DisplayName("Member: Forbidden from staff circulation desk (403)")
    void testMemberForbiddenFromCirculationDesk() throws Exception {
        mockMvc.perform(get("/circulation/desk"))
                .andExpect(status().isForbidden());
    }

    // 3. LIBRARIAN ACCESS TESTS
    @Test
    @WithMockUser(username = "librarian@campus.edu", roles = {"LIBRARIAN"})
    @DisplayName("Librarian: Can access circulation desk")
    void testLibrarianCanAccessCirculationDesk() throws Exception {
        mockMvc.perform(get("/circulation/desk"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "librarian@campus.edu", roles = {"LIBRARIAN"})
    @DisplayName("Librarian: Forbidden from admin user management (403)")
    void testLibrarianForbiddenFromAdminUsers() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());
    }

    // 4. ADMIN ACCESS TESTS
    @Test
    @WithMockUser(username = "admin@campus.edu", roles = {"ADMIN"})
    @DisplayName("Admin: Can access admin user management")
    void testAdminCanAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@campus.edu", roles = {"ADMIN"})
    @DisplayName("Admin: Can access circulation desk")
    void testAdminCanAccessCirculationDesk() throws Exception {
        mockMvc.perform(get("/circulation/desk"))
                .andExpect(status().isOk());
    }

    // 5. SUPER_ADMIN ACCESS TESTS
    @Test
    @WithMockUser(username = "super@campus.edu", roles = {"SUPER_ADMIN", "ADMIN"})
    @DisplayName("Super Admin: Can access all administrative areas")
    void testSuperAdminAccess() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
    }
}
