package com.college.library.security;

import com.college.library.entity.User;
import com.college.library.repository.UserRepository;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class LoginSecurityService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int LOCKOUT_MINUTES = 15;

    private final UserRepository userRepository;

    public LoginSecurityService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordFailedAttempt(String email, String tenantId) {
        if (email == null || email.isBlank()) {
            return;
        }
        userRepository.findByTenantIdAndEmail(tenantId, email.trim().toLowerCase()).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(OffsetDateTime.now().plusMinutes(LOCKOUT_MINUTES));
            }
            userRepository.save(user);
        });
    }

    @Transactional
    public void resetFailedAttempts(String email, String tenantId) {
        if (email == null || email.isBlank()) {
            return;
        }
        userRepository.findByTenantIdAndEmail(tenantId, email.trim().toLowerCase()).ifPresent(user -> {
            if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        });
    }

    @Transactional
    public void checkLockout(User user) {
        if (user.getLockedUntil() != null) {
            if (user.getLockedUntil().isAfter(OffsetDateTime.now())) {
                throw new LockedException("Your account is locked until " + user.getLockedUntil()
                        + " due to " + MAX_FAILED_ATTEMPTS + " consecutive failed login attempts.");
            } else {
                // Lockout duration has expired, reset state
                user.setLockedUntil(null);
                user.setFailedLoginAttempts(0);
                userRepository.save(user);
            }
        }
    }
}
