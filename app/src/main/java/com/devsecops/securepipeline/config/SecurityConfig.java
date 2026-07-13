package com.devsecops.securepipeline.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import	org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * SecurityConfig — Spring Security Configuration
 * ─────────────────────────────────────────────────────────────────────────────
 *
 * <p>A minimal security configuration that:
 * <ul>
 *   <li>Permits all requests (this is a demo app — in production you must add
 *       proper authentication and authorization rules).</li>
 *   <li>Disables CSRF (appropriate for stateless REST APIs).</li>
 *   <li>Sets session creation policy to STATELESS.</li>
 * </ul>
 *
 * <p><b>PRODUCTION NOTE:</b> Replace the permitAll() strategy with proper
 * authentication (OAuth2, JWT, or session-based). The actuator endpoints should
 * be restricted to internal networks via Kubernetes NetworkPolicies.
 *
 * @author DevSecOps Team
 * @version 1.0.0
 * @since 1.0.0
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless REST API
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()); // Demo: open access

        return http.build();
    }
}