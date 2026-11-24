package com.college.library.security;

import com.college.library.tenant.TenantContext;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationEventsListener {

    private final LoginSecurityService loginSecurityService;

    public AuthenticationEventsListener(LoginSecurityService loginSecurityService) {
        this.loginSecurityService = loginSecurityService;
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        String email = event.getAuthentication().getName();
        String tenantId = TenantContext.getTenantId();
        loginSecurityService.recordFailedAttempt(email, tenantId);
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();
        String tenantId = TenantContext.getTenantId();
        loginSecurityService.resetFailedAttempts(email, tenantId);
    }
}
