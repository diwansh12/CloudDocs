package com.clouddocs.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class StaticResourceConfiguration implements WebMvcConfigurer {
    
    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;
    
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // Convert to absolute path and ensure it ends with /
        String uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        
        // ✅ CRITICAL: Map the exact URL pattern to your upload directory
        registry.addResourceHandler("/api/users/profile/picture/**")
                .addResourceLocations(uploadPath)
                .setCachePeriod(0) // No cache for development
                .resourceChain(true);
        
        System.out.println("✅ Static resource mapping configured:");
        System.out.println("   URL Pattern: /api/users/profile/picture/**");
        System.out.println("   File Location: " + uploadPath);
        System.out.println("   Upload directory exists: " + Paths.get(uploadDir).toFile().exists());
    }
}
