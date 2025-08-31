package com.clouddocs.backend.config;

import com.clouddocs.backend.security.JwtAuthenticationFilter;
import com.clouddocs.backend.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ✅ FIXED: Updated to match your frontend domain
    @Value("${app.cors.allowed-origins:https://cloud-docs-tan.vercel.app,http://localhost:3000}")
    private String allowedOrigins;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(List.of(authenticationProvider()));
    }

    /**
     * ✅ FIXED: CORS configuration for preflight requests
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // ✅ CRITICAL FIX: Use setAllowedOrigins instead of setAllowedOriginPatterns
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        for (String origin : origins) {
            configuration.addAllowedOrigin(origin.trim());
        }
        
        // ✅ CRITICAL: Include OPTIONS for preflight requests
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));
        
        // ✅ Allow all headers including custom ones
        configuration.addAllowedHeader("*");
        
        // ✅ CRITICAL: Enable credentials for authentication
        configuration.setAllowCredentials(true);
        
        // ✅ Expose headers that frontend might need
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * ✅ FIXED: Security filter chain with proper CORS handling
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            // ✅ CRITICAL FIX: Enable CORS - this was commented out!
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // ✅ CSRF disabled for REST APIs
            .csrf(AbstractHttpConfigurer::disable)
            
            // ✅ Stateless session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
            // ✅ FIXED: Request authorization with proper endpoint mapping
            .authorizeHttpRequests(auth -> auth
                // ✅ CRITICAL: Allow OPTIONS requests for all paths (preflight)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Public static resources
                .requestMatchers(
                    "/favicon.ico",
                    "/error",
                    "/actuator/health",
                    "/actuator/info",
                    "/static/**",
                    "/public/**",
                    "/uploads/**",
                    "/health" // Add health endpoint
                ).permitAll()
                
                // ✅ FIXED: Authentication endpoints - corrected paths
                .requestMatchers(
                    "/auth/signin",      // ✅ FIXED: was /api/auth/login
                    "/auth/signup",      // ✅ FIXED: was /api/auth/register
                    "/auth/refresh",
                    "/auth/forgot-password",
                    "/auth/reset-password",
                    "/public/**"
                ).permitAll()
                
                // User endpoints require authentication
                .requestMatchers(
                    "/users/**",
                    "/workflows/**",
                    "/documents/**",
                    "/notifications/**",
                    "/settings/**",
                    "/audit/**"
                ).authenticated()
                
                // Admin endpoints require admin role
                .requestMatchers(
                    "/admin/**",
                    "/actuator/**"
                ).hasRole("ADMIN")
                
                // Test endpoints (remove in production)
                .requestMatchers("/test/**").permitAll()
                
                // All other requests require authentication
                .anyRequest().authenticated())
                
            // ✅ Exception handling
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    
                    // ✅ Add CORS headers to error responses
                    response.setHeader("Access-Control-Allow-Origin", "https://cloud-docs-tan.vercel.app");
                    response.setHeader("Access-Control-Allow-Credentials", "true");
                    
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
                    
                    // ✅ Add CORS headers to error responses
                    response.setHeader("Access-Control-Allow-Origin", "https://cloud-docs-tan.vercel.app");
                    response.setHeader("Access-Control-Allow-Credentials", "true");
                    
                    String errorResponse = String.format(
                        "{\"error\":\"Forbidden\",\"message\":\"Access denied\",\"timestamp\":\"%s\",\"path\":\"%s\"}",
                        java.time.Instant.now().toString(),
                        request.getRequestURI()
                    );
                    
                    response.getWriter().write(errorResponse);
                })
            )
                
            // ✅ Authentication provider
            .authenticationProvider(authenticationProvider())
            
            // ✅ JWT filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            .build();
    }
}
