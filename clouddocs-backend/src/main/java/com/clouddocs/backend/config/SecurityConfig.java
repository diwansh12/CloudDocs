package com.clouddocs.backend.config;

import com.clouddocs.backend.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins:https://cloud-docs-tan.vercel.app,http://localhost:3000}")
    private String allowedOrigins;

    // ✅ PRODUCTION: BCrypt Password Encoder
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Production-grade BCrypt with strength 12 (recommended for 2024+)
        return new BCryptPasswordEncoder(12);
    }

    // ... rest of your existing configuration remains the same
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        for (String origin : origins) {
            configuration.addAllowedOriginPattern(origin.trim());
        }
        
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
        
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "Content-Disposition"));
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                    "/favicon.ico", "/error", "/actuator/health", "/actuator/info",
                    "/static/**", "/public/**", "/uploads/**", "/health"
                ).permitAll()
                .requestMatchers(
                    "/api/auth/**", "/auth/**"
                ).permitAll()
                .requestMatchers("/api/documents/shared/**").permitAll()
                .requestMatchers("/api/users/profile/picture/**").permitAll()
                .requestMatchers("/api/ocr/**").authenticated()
                .requestMatchers("/api/search/**").authenticated()
                .requestMatchers("/api/users/**").authenticated()
                .requestMatchers("/api/documents/**").authenticated()
                .requestMatchers("/api/dashboard/**").authenticated()
                .requestMatchers("/api/workflows/**").authenticated()
                .requestMatchers("/api/notifications/**").authenticated()
                .requestMatchers("/api/settings/**").authenticated()
                .requestMatchers("/api/audit/**").authenticated()
                .requestMatchers(
                    "/users/**", "/documents/**", "/workflows/**", 
                    "/notifications/**", "/settings/**", "/audit/**"
                ).authenticated()
                .requestMatchers("/admin/**", "/actuator/**").hasRole("ADMIN")
                .requestMatchers("/test/**").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    
                    String origin = request.getHeader("Origin");
                    if (origin != null && (origin.contains("vercel.app") || origin.contains("localhost"))) {
                        response.setHeader("Access-Control-Allow-Origin", origin);
                        response.setHeader("Access-Control-Allow-Credentials", "true");
                    }
                    
                    String errorResponse = String.format(
                        "{\"error\":\"Unauthorized\",\"message\":\"Authentication required\",\"timestamp\":\"%s\",\"path\":\"%s\"}",
                        java.time.Instant.now().toString(), request.getRequestURI());
                    
                    response.getWriter().write(errorResponse);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    
                    String origin = request.getHeader("Origin");
                    if (origin != null && (origin.contains("vercel.app") || origin.contains("localhost"))) {
                        response.setHeader("Access-Control-Allow-Origin", origin);
                        response.setHeader("Access-Control-Allow-Credentials", "true");
                    }
                    
                    String errorResponse = String.format(
                        "{\"error\":\"Forbidden\",\"message\":\"Access denied\",\"timestamp\":\"%s\",\"path\":\"%s\"}",
                        java.time.Instant.now().toString(), request.getRequestURI());
                    
                    response.getWriter().write(errorResponse);
                }))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
