package com.college.library.config;

import com.college.library.entity.Role;
import com.college.library.entity.User;
import com.college.library.repository.UserRepository;
import com.college.library.security.CustomUserDetails;
import com.college.library.security.LoginSecurityService;
import com.college.library.tenant.TenantContext;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final LoginSecurityService loginSecurityService;

    public CustomUserDetailsService(UserRepository userRepository, LoginSecurityService loginSecurityService) {
        this.userRepository = userRepository;
        this.loginSecurityService = loginSecurityService;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String tenantId = TenantContext.getTenantId();
        User user = userRepository.findByTenantIdAndEmail(tenantId, email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email '" + email + "' in library tenant '" + tenantId + "'"));

        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled: " + email);
        }

        // Enforce account lockout if threshold reached and duration not expired
        loginSecurityService.checkLockout(user);

        List<GrantedAuthority> authorities = new ArrayList<>();
        Role role = user.getRole();

        // Map primary and backward-compatible authorities
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

        if (role == Role.SUPER_ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_LIBRARY_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
        } else if (role == Role.LIBRARY_ADMIN || role == Role.ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_LIBRARY_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
        } else if (role == Role.LIBRARIAN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
        } else if (role == Role.MEMBER || role == Role.STUDENT) {
            authorities.add(new SimpleGrantedAuthority("ROLE_STUDENT"));
            authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
        }

        return new CustomUserDetails(user, authorities);
    }
}
