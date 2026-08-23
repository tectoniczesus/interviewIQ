package com.interviewiq.config;

import com.interviewiq.security.ClerkAuthenticationFilter;
import com.interviewiq.security.ClerkJwtValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ClerkJwtValidator clerkJwtValidator;

    public SecurityConfig(ClerkJwtValidator clerkJwtValidator) {
        this.clerkJwtValidator = clerkJwtValidator;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Spring MVC is on the classpath and no CorsConfigurationSource bean is
                // defined, so Spring Security auto-picks-up WebConfig's addCorsMappings
                // and handles preflight OPTIONS requests before the auth checks below run.
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable()) // stateless JSON API authenticated via bearer tokens, not cookies
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/health", "/api/public/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(new ClerkAuthenticationFilter(clerkJwtValidator),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}