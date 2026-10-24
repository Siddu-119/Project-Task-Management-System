package com.projectmgmt.config;

import com.projectmgmt.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
        http
            .csrf(csrf -> csrf.disable())          // not needed: we use JWT headers, not cookies
            .cors(Customizer.withDefaults())       // uses CorsConfig
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                // Only admins can create / edit / delete projects and tasks (same as V1 UI)
                .requestMatchers(HttpMethod.POST, "/api/projects/**", "/api/tasks").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/projects/**", "/api/tasks/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/projects/**", "/api/tasks/**").hasRole("ADMIN")
                // Reports are admin-only
                .requestMatchers("/api/reports/**").hasRole("ADMIN")
                // Everything else just needs a valid login
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> writeJson(res, 401, "Please log in to continue."))
                .accessDeniedHandler((req, res, e) -> writeJson(res, 403, "You do not have permission to do this.")))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeJson(jakarta.servlet.http.HttpServletResponse res, int status, String message)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
