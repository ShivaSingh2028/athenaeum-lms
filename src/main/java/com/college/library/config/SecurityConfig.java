package com.college.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/login", "/register",
                    "/css/**", "/images/**", "/favicon.ico", "/manifest.json", "/sw.js", "/offline.html",
                    "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html",
                    "/actuator/health", "/error"
                ).permitAll()
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "LIBRARY_ADMIN")
                .requestMatchers("/circulation/**").hasAnyRole("ADMIN", "LIBRARIAN", "SUPER_ADMIN", "LIBRARY_ADMIN")
                .requestMatchers("/barcodes/**").hasAnyRole("ADMIN", "LIBRARIAN", "SUPER_ADMIN", "LIBRARY_ADMIN")
                .requestMatchers("/books/new", "/books/*/edit", "/books/*/delete", "/books/import-csv", "/books/*/copies/**").hasAnyRole("ADMIN", "LIBRARIAN", "SUPER_ADMIN", "LIBRARY_ADMIN")
                .requestMatchers("/transactions/my").hasAnyRole("MEMBER", "STUDENT")
                .requestMatchers("/transactions/**").hasAnyRole("ADMIN", "LIBRARIAN", "SUPER_ADMIN", "LIBRARY_ADMIN")
                .requestMatchers("/reports/**").hasAnyRole("ADMIN", "LIBRARIAN", "SUPER_ADMIN", "LIBRARY_ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .maximumSessions(2)
                .maxSessionsPreventsLogin(false)
            )
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; img-src 'self' data: https:; font-src 'self' https: data:; frame-ancestors 'none';")
                )
                .frameOptions(frame -> frame.deny())
                .xssProtection(xss -> xss.disable())
                .contentTypeOptions(content -> {})
                .referrerPolicy(ref -> ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            );

        return http.build();
    }
}
