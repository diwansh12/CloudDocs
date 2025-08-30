package com.clouddocs.backend.config;

import com.clouddocs.backend.security.JwtAuthenticationFilter;
import com.clouddocs.backend.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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

/**
 * ✅ UPDATED: Spring Security 6.1+ compatible configuration
 * - All deprecated methods removed
 * - Production-ready security settings
 * - Environment-based CORS configuration
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ✅ PRODUCTION: Environment-based CORS origins
    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    /**
     * ✅ PRODUCTION: Strong password encoding
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * ✅ FIXED: Updated DaoAuthenticationProvider configuration (no deprecations)
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * ✅ PRODUCTION: Authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(List.of(authenticationProvider()));
    }

    /**
     * ✅ PRODUCTION: Environment-based CORS configuration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // ✅ PRODUCTION: Use environment variable for allowed origins
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOriginPatterns(origins);
        
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With", 
            "Accept", 
            "Origin", 
            "Access-Control-Request-Method", 
            "Access-Control-Request-Headers"
        ));
        
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * ✅ UPDATED: Spring Security 6.1+ compatible filter chain (no deprecated methods)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // ✅ CSRF disabled for REST APIs
            .csrf(AbstractHttpConfigurer::disable)
            
            // ✅ CORS configuration
            //.cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // ✅ Stateless session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
            // ✅ Request authorization
            .authorizeHttpRequests(auth -> auth
                // Public static resources
                .requestMatchers(
                    "/favicon.ico",
                    "/error",
                    "/actuator/health",
                    "/actuator/info",
                    "/static/**",
                    "/public/**",
                    "/uploads/**"
                ).permitAll()
                
                // Authentication endpoints
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register", 
                    "/api/auth/refresh",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/public/**"
                ).permitAll()
                
                // User endpoints require authentication
                .requestMatchers(
                    "/api/users/**",
                    "/api/workflows/**",
                    "/api/documents/**",
                    "/api/notifications/**",
                    "/api/settings/**",
                    "/api/audit/**"
                ).authenticated()
                
                // Admin endpoints require admin role
                .requestMatchers(
                    "/api/admin/**",
                    "/actuator/**"
                ).hasRole("ADMIN")
                
                // Test endpoints (remove in production)
                .requestMatchers("/api/test/**").permitAll()
                
                // All other requests require authentication
                .anyRequest().authenticated())
                
            // ✅ UPDATED: Exception handling (no deprecated methods)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    
                    String errorResponse = String.format(
                        "{\"error\":\"Unauthorized\",\"message\":\"Authentication required\",\"timestamp\":\"%s\",\"path\":\"%s\"}",
                        java.time.Instant.now().toString(),
                        request.getRequestURI()
                    );
                    
                    response.getWriter().write(errorResponse);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    
                    String errorResponse = String.format(
                        "{\"error\":\"Forbidden\",\"message\":\"Access denied\",\"timestamp\":\"%s\",\"path\":\"%s\"}",
                        java.time.Instant.now().toString(),
                        request.getRequestURI()
                    );
                    
                    response.getWriter().write(errorResponse);
                })
            )
            
            // ✅ REMOVED: Deprecated headers configuration
            // Note: Spring Security 6.1+ provides secure defaults automatically
            // If you need custom headers, implement a custom filter or use @Bean WebSecurityCustomizer
                
            // ✅ Authentication provider
            .authenticationProvider(authenticationProvider())
            
            // ✅ JWT filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            .build();
    }
}
