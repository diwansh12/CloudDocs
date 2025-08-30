// src/main/java/com/clouddocs/backend/config/MethodSecurityConfig.java
package com.clouddocs.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity // enables @PreAuthorize, @PostAuthorize, etc.
public class MethodSecurityConfig {}
