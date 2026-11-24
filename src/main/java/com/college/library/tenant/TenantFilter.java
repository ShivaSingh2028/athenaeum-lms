package com.college.library.tenant;

import com.college.library.security.CustomUserDetails;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantFilter implements Filter {

    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String TENANT_SESSION_ATTR = "ACTIVE_TENANT_ID";

    @Value("${app.tenant.base-domain:athenaeum.edu}")
    private String baseDomain;

    private final Environment environment;

    public TenantFilter(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            String tenantId = resolveTenantId(httpRequest);
            TenantContext.setTenantId(tenantId);
            httpRequest.setAttribute("currentTenantId", tenantId);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    public String resolveTenantId(HttpServletRequest request) {
        // 1. Authenticated user tenant from SecurityContext / session
        // An authenticated user must NEVER be able to switch tenant or be overridden
        String authTenant = resolveAuthenticatedUserTenant(request);
        if (authTenant != null && !authTenant.isBlank()) {
            return authTenant.toLowerCase();
        }

        // 2. Subdomain on configured base domain
        String subdomainTenant = resolveSubdomainTenant(request.getServerName());
        if (subdomainTenant != null && !subdomainTenant.isBlank()) {
            return subdomainTenant.toLowerCase();
        }

        // 3. Header allowed ONLY under "dev" or "test" profiles for unauthenticated requests
        if (isDevOrTestProfile()) {
            String headerTenant = request.getHeader(TENANT_HEADER);
            if (headerTenant != null && !headerTenant.isBlank()) {
                return headerTenant.trim().toLowerCase();
            }
        }

        // 4. Default tenant fallback
        return TenantContext.DEFAULT_TENANT_ID;
    }

    private String resolveAuthenticatedUserTenant(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isValidUserAuth(auth)) {
            if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                return userDetails.getTenantId();
            }
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            Object scObj = session.getAttribute("SPRING_SECURITY_CONTEXT");
            if (scObj instanceof SecurityContext sc && isValidUserAuth(sc.getAuthentication())) {
                Object principal = sc.getAuthentication().getPrincipal();
                if (principal instanceof CustomUserDetails userDetails) {
                    return userDetails.getTenantId();
                }
            }
            Object sessionTenant = session.getAttribute(TENANT_SESSION_ATTR);
            if (sessionTenant instanceof String s && !s.isBlank()) {
                return s.trim();
            }
        }
        return null;
    }

    private boolean isValidUserAuth(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    private String resolveSubdomainTenant(String serverName) {
        if (serverName == null || serverName.isBlank() || serverName.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            return null;
        }

        String domain = baseDomain != null && !baseDomain.isBlank() ? baseDomain.trim().toLowerCase() : "athenaeum.edu";
        String server = serverName.trim().toLowerCase();

        if (server.endsWith("." + domain)) {
            String sub = server.substring(0, server.length() - domain.length() - 1);
            if (!sub.isBlank() && !sub.equalsIgnoreCase("www") && !sub.equalsIgnoreCase("api")) {
                String[] parts = sub.split("\\.");
                return parts[parts.length - 1];
            }
        } else if (server.endsWith(".localhost")) {
            String sub = server.substring(0, server.length() - ".localhost".length());
            if (!sub.isBlank() && !sub.equalsIgnoreCase("www") && !sub.equalsIgnoreCase("api")) {
                return sub;
            }
        } else {
            String[] parts = server.split("\\.");
            if (parts.length >= 3 && !parts[0].equalsIgnoreCase("www") && !parts[0].equalsIgnoreCase("api")) {
                return parts[0];
            }
        }
        return null;
    }

    private boolean isDevOrTestProfile() {
        return environment != null && environment.acceptsProfiles(Profiles.of("dev", "test"));
    }
}
