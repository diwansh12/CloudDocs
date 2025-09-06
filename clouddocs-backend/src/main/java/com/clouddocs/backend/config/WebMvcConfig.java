 /*package com.clouddocs.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        try {
            // ✅ Ensure directories exist
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path profilePicturesPath = uploadPath.resolve("profile-pictures");
            Files.createDirectories(profilePicturesPath);
            
            // ✅ Configure static resource handler with absolute path
            registry.addResourceHandler("/api/users/profile/picture/**")
                    .addResourceLocations("file:" + uploadPath.toString() + "/")
                    .setCachePeriod(3600)
                    .resourceChain(true);
                    
            System.out.println("🔧 Static resource handler configured:");
            System.out.println("    Pattern: /api/users/profile/picture/**");
            System.out.println("    Location: file:" + uploadPath.toString() + "/");
            System.out.println("    Profile Pictures: " + profilePicturesPath.toString());
            
        } catch (IOException e) {
            System.err.println("❌ Failed to setup resource handler: " + e.getMessage());
            e.printStackTrace();
        }
    }
}*/
