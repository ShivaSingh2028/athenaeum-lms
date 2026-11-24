package com.college.library.service;

import com.college.library.entity.Role;
import com.college.library.entity.User;
import com.college.library.repository.UserRepository;
import com.college.library.tenant.TenantContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String name, String email, String rawPassword, String phone) {
        String tenantId = TenantContext.getTenantId();
        if (userRepository.existsByTenantIdAndEmail(tenantId, email.trim().toLowerCase())) {
            throw new IllegalArgumentException("An account with this email already exists in this library.");
        }
        User user = new User();
        user.setTenantId(tenantId);
        user.setName(name.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhone(phone);
        user.setRole(Role.MEMBER);
        user.setEmailVerified(false);
        return userRepository.save(user);
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in security context.");
        }
        String email = auth.getName();
        return userRepository.findByTenantIdAndEmail(TenantContext.getTenantId(), email)
                .orElseThrow(() -> new IllegalStateException("User not found for active tenant: " + email));
    }

    public List<User> findAll() {
        return userRepository.findByTenantId(TenantContext.getTenantId());
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + id));
    }

    public java.util.Optional<User> findByEmail(String email) {
        if (email == null) return java.util.Optional.empty();
        return userRepository.findByTenantIdAndEmail(TenantContext.getTenantId(), email.trim().toLowerCase());
    }

    @Transactional
    public User save(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}
