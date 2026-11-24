package com.college.library.security;

import com.college.library.config.CustomUserDetailsService;
import com.college.library.entity.Role;
import com.college.library.entity.User;
import com.college.library.fake.FakeUserRepository;
import com.college.library.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Phase 1: Account Lockout & Login Rate Limiting Unit Tests")
class AccountLockoutAndRateLimiterTest {

    private FakeUserRepository userRepository;
    private LoginSecurityService loginSecurityService;
    private CustomUserDetailsService userDetailsService;
    private AuthenticationEventsListener eventsListener;
    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        loginSecurityService = new LoginSecurityService(userRepository);
        userDetailsService = new CustomUserDetailsService(userRepository, loginSecurityService);
        eventsListener = new AuthenticationEventsListener(loginSecurityService);

        TenantContext.setTenantId("campus-main");

        testUser = new User("Student John", "john@campus.edu", "$2a$12$fakeHashedPassword", Role.MEMBER);
        testUser.setId(301L);
        testUser.setTenantId("campus-main");
        testUser.setEnabled(true);
        testUser.setFailedLoginAttempts(0);
        testUser.setLockedUntil(null);
        userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Authentication createAuth(String email) {
        return new UsernamePasswordAuthenticationToken(email, "rawpassword");
    }

    @Test
    @DisplayName("Verify Account Lockout: 5 failed attempts locks account for 15 minutes")
    void testAccountLockoutAfterFiveFailures() {
        // Fire 4 failed login events
        for (int i = 1; i <= 4; i++) {
            eventsListener.onFailure(new AuthenticationFailureBadCredentialsEvent(
                    createAuth("john@campus.edu"), new org.springframework.security.authentication.BadCredentialsException("Bad")));
            User u = userRepository.findByTenantIdAndEmail("campus-main", "john@campus.edu").get();
            assertEquals(i, u.getFailedLoginAttempts(), "Failed attempt count should increment to " + i);
            assertNull(u.getLockedUntil(), "Account should not be locked before 5 attempts");
        }

        // 5th failed attempt triggers lockout
        eventsListener.onFailure(new AuthenticationFailureBadCredentialsEvent(
                createAuth("john@campus.edu"), new org.springframework.security.authentication.BadCredentialsException("Bad")));

        User lockedUser = userRepository.findByTenantIdAndEmail("campus-main", "john@campus.edu").get();
        assertEquals(5, lockedUser.getFailedLoginAttempts());
        assertNotNull(lockedUser.getLockedUntil(), "User should have lockedUntil set");
        assertTrue(lockedUser.getLockedUntil().isAfter(OffsetDateTime.now().plusMinutes(14)),
                "User should be locked for ~15 minutes");

        // Subsequent loadUserByUsername must throw LockedException
        assertThrows(LockedException.class, () -> {
            userDetailsService.loadUserByUsername("john@campus.edu");
        });
    }

    @Test
    @DisplayName("Verify Auto-Unlock: After 15 minutes elapse, account unlocks automatically")
    void testAutoUnlockAfterExpiry() {
        // Set lockedUntil in the past (e.g. 20 minutes ago)
        testUser.setFailedLoginAttempts(5);
        testUser.setLockedUntil(OffsetDateTime.now().minusMinutes(20));
        userRepository.save(testUser);

        // loadUserByUsername should auto-unlock and succeed
        UserDetails details = assertDoesNotThrow(() -> {
            return userDetailsService.loadUserByUsername("john@campus.edu");
        });

        assertNotNull(details);
        User restored = userRepository.findByTenantIdAndEmail("campus-main", "john@campus.edu").get();
        assertEquals(0, restored.getFailedLoginAttempts(), "Attempts should be reset to 0 after unlock");
        assertNull(restored.getLockedUntil(), "lockedUntil should be cleared");
    }

    @Test
    @DisplayName("Verify Success Reset: Successful login resets failed attempts")
    void testSuccessfulLoginResetsFailures() {
        // 3 failed attempts
        for (int i = 0; i < 3; i++) {
            eventsListener.onFailure(new AuthenticationFailureBadCredentialsEvent(
                    createAuth("john@campus.edu"), new org.springframework.security.authentication.BadCredentialsException("Bad")));
        }

        assertEquals(3, userRepository.findByTenantIdAndEmail("campus-main", "john@campus.edu").get().getFailedLoginAttempts());

        // Successful authentication event
        eventsListener.onSuccess(new AuthenticationSuccessEvent(createAuth("john@campus.edu")));

        User userAfterSuccess = userRepository.findByTenantIdAndEmail("campus-main", "john@campus.edu").get();
        assertEquals(0, userAfterSuccess.getFailedLoginAttempts(), "Successful login must reset failed attempts counter");
        assertNull(userAfterSuccess.getLockedUntil());
    }

    @Test
    @DisplayName("Verify LoginRateLimiterFilter: Enforces 10 requests/min per IP threshold")
    void testRateLimiterBlocksAfterThreshold() throws Exception {
        LoginRateLimiterFilter filter = new LoginRateLimiterFilter();

        String attackerIp = "198.51.100.42";

        // First 10 POST /login requests should succeed through filter chain
        for (int i = 1; i <= 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/login");
            req.setRemoteAddr(attackerIp);
            MockHttpServletResponse resp = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(req, resp, chain);
            assertEquals(200, resp.getStatus(), "Request " + i + " within rate limit must pass");
        }

        // 11th request from same IP must be rate-limited (HTTP 429 for API)
        MockHttpServletRequest req11 = new MockHttpServletRequest("POST", "/login");
        req11.setRemoteAddr(attackerIp);
        req11.addHeader("Accept", "application/json");
        MockHttpServletResponse resp11 = new MockHttpServletResponse();
        MockFilterChain chain11 = new MockFilterChain();

        filter.doFilter(req11, resp11, chain11);
        assertEquals(429, resp11.getStatus(), "11th request must receive HTTP 429 Too Many Requests");

        // 12th request from browser (text/html) must redirect to /login?error=ratelimit
        MockHttpServletRequest req12 = new MockHttpServletRequest("POST", "/login");
        req12.setRemoteAddr(attackerIp);
        req12.addHeader("Accept", "text/html,application/xhtml+xml");
        MockHttpServletResponse resp12 = new MockHttpServletResponse();
        MockFilterChain chain12 = new MockFilterChain();

        filter.doFilter(req12, resp12, chain12);
        assertEquals(302, resp12.getStatus());
        assertEquals("/login?error=ratelimit", resp12.getHeader("Location"));

        // A different IP address is NOT blocked
        MockHttpServletRequest reqOther = new MockHttpServletRequest("POST", "/login");
        reqOther.setRemoteAddr("203.0.113.88");
        MockHttpServletResponse respOther = new MockHttpServletResponse();
        MockFilterChain chainOther = new MockFilterChain();

        filter.doFilter(reqOther, respOther, chainOther);
        assertEquals(200, respOther.getStatus(), "Different IP should not be blocked");
    }
}
