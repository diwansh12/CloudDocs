package com.clouddocs.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {
    
    @Value("${app.firebase.service-account-key}")
    private String serviceAccountKey;
    
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (serviceAccountKey == null || serviceAccountKey.trim().isEmpty()) {
            System.out.println("Firebase service account key not configured");
            return null;
        }
        
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                // Convert the JSON string to InputStream
                InputStream serviceAccount = new ByteArrayInputStream(serviceAccountKey.getBytes());
                
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                        
                return FirebaseApp.initializeApp(options);
            }
            return FirebaseApp.getInstance();
        } catch (Exception e) {
            System.err.println("Failed to initialize Firebase: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
