package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.request.LoginRequest;
import com.clouddocs.backend.dto.request.SignupRequest;
import com.clouddocs.backend.dto.response.JwtResponse;
import com.clouddocs.backend.dto.response.MessageResponse;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.Role;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"}, 
             maxAge = 3600, allowCredentials = "true")
@RestController
@RequestMapping("/auth")  // Note: Removed /api since you're using context-path=/api
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder encoder;
    
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, 
                                            BindingResult bindingResult) {
        try {
            logger.info("🔑 Login request received for: {}", loginRequest.getUsername());
            
            // Validate input
            if (bindingResult.hasErrors()) {
                logger.warn("⚠️ Validation errors for user: {}", loginRequest.getUsername());
                bindingResult.getAllErrors().forEach(error -> 
                    logger.warn("Validation error: {}", error.getDefaultMessage()));
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Invalid input data"));
            }
            
            // Check for null values
            if (loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Username and password are required"));
            }
            
            // Debug logging
            logger.debug("Username: {}, Password length: {}", 
                loginRequest.getUsername(), 
                loginRequest.getPassword() != null ? loginRequest.getPassword().length() : "null");
            
            // Authenticate using AuthService
            JwtResponse jwtResponse = authService.authenticate(loginRequest);
            
            logger.info("✅ Login successful for user: {}", loginRequest.getUsername());
            return ResponseEntity.ok(jwtResponse);
            
        } catch (Exception e) {
            logger.error("❌ Login failed for user: {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Invalid username or password"));
        }
    }
    
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest,
                                        BindingResult bindingResult) {
        try {
            logger.info("📝 Registration request for: {}", signUpRequest.getUsername());
            
            // Validate input
            if (bindingResult.hasErrors()) {
                logger.warn("⚠️ Validation errors for registration: {}", signUpRequest.getUsername());
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Invalid input data"));
            }
            
            // Check if username exists
            if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                logger.warn("⚠️ Username already taken: {}", signUpRequest.getUsername());
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Username is already taken!"));
            }
            
            // Check if email exists
            if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                logger.warn("⚠️ Email already in use: {}", signUpRequest.getEmail());
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Email is already in use!"));
            }
            
            // Create new user
            User user = new User();
            user.setUsername(signUpRequest.getUsername());
            user.setEmail(signUpRequest.getEmail());
            user.setPassword(encoder.encode(signUpRequest.getPassword()));
            user.setFirstName(signUpRequest.getFirstName());
            user.setLastName(signUpRequest.getLastName());
            user.setCreatedAt(LocalDateTime.now());
            user.setEnabled(true);
            user.setRole(Role.USER);
            
            User savedUser = userRepository.save(user);
            logger.info("✅ User registered successfully: ID={}, Username={}, Email={}", 
                savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());
            
            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
            
        } catch (Exception e) {
            logger.error("❌ Registration failed for user: {}", signUpRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Registration failed: " + e.getMessage()));
        }
    }
    
    // Health check endpoint
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("authService", authService != null ? "INJECTED" : "NULL");
        status.put("userRepository", userRepository != null ? "INJECTED" : "NULL");
        status.put("userCount", authService != null ? authService.getUserCount() : 0);
        status.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(status);
    }
    
    // Debug endpoint (remove in production)
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debug() {
        Map<String, Object> info = new HashMap<>();
        info.put("userCount", userRepository.count());
        
        // List first 5 users for debugging
        List<User> sampleUsers = userRepository.findAll().stream()
            .limit(5)
            .map(user -> {
                User sanitized = new User();
                sanitized.setId(user.getId());
                sanitized.setUsername(user.getUsername());
                sanitized.setEmail(user.getEmail());
                sanitized.setFirstName(user.getFirstName());
                sanitized.setLastName(user.getLastName());
                sanitized.setRole(user.getRole());
                return sanitized;
            })
            .toList();
            
        info.put("sampleUsers", sampleUsers);
        return ResponseEntity.ok(info);
    }

    // Debug endpoint to check user existence and password format
@GetMapping("/debug/user/{username}")
public ResponseEntity<Map<String, Object>> debugUser(@PathVariable String username) {
    try {
        Map<String, Object> info = new HashMap<>();
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(username);
        }
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            info.put("userExists", true);
            info.put("userId", user.getId());
            info.put("username", user.getUsername());
            info.put("email", user.getEmail());
            info.put("enabled", user.isEnabled());
            info.put("role", user.getRole());
            
            // Show password format (first 20 chars for security)
            String passwordPreview = user.getPassword() != null ? 
                user.getPassword().substring(0, Math.min(20, user.getPassword().length())) + "..." : "NULL";
            info.put("passwordFormat", passwordPreview);
            info.put("passwordLength", user.getPassword() != null ? user.getPassword().length() : 0);
            info.put("startsWithBcrypt", user.getPassword() != null && user.getPassword().startsWith("{bcrypt}"));
            
        } else {
            info.put("userExists", false);
            info.put("totalUsers", userRepository.count());
        }
        
        return ResponseEntity.ok(info);
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}

// Test password encoding
@PostMapping("/debug/test-password")
public ResponseEntity<Map<String, Object>> testPassword(@RequestBody Map<String, String> request) {
    try {
        String rawPassword = request.get("password");
        String encodedPassword = encoder.encode(rawPassword);
        
        Map<String, Object> result = new HashMap<>();
        result.put("rawPassword", rawPassword);
        result.put("encodedPassword", encodedPassword);
        result.put("encoderType", encoder.getClass().getSimpleName());
        result.put("matchesItself", encoder.matches(rawPassword, encodedPassword));
        
        return ResponseEntity.ok(result);
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}

@PostMapping("/debug/fix-user-password")
public ResponseEntity<?> fixUserPassword(@RequestBody Map<String, String> request) {
    try {
        String username = request.get("username");
        String rawPassword = request.get("password");
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(username);
        }
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Check if password needs migration (no {bcrypt} prefix)
            if (!user.getPassword().startsWith("{bcrypt}")) {
                // Verify the raw password against current hash first
                BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();
                String storedHash = user.getPassword();
                
                if (bcrypt.matches(rawPassword, storedHash)) {
                    // Add {bcrypt} prefix to existing valid hash
                    String migratedPassword = "{bcrypt}" + storedHash;
                    user.setPassword(migratedPassword);
                    userRepository.save(user);
                    
                    logger.info("✅ Migrated password for user: {}", username);
                    
                    return ResponseEntity.ok(Map.of(
                        "message", "Password migrated successfully",
                        "username", username,
                        "oldFormat", storedHash.substring(0, 15) + "...",
                        "newFormat", migratedPassword.substring(0, 20) + "..."
                    ));
                } else {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid password provided"));
                }
            } else {
                return ResponseEntity.ok(Map.of(
                    "message", "Password already in correct format",
                    "username", username
                ));
            }
        } else {
            return ResponseEntity.notFound().build();
        }
    } catch (Exception e) {
        logger.error("Error fixing user password", e);
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}

@GetMapping("/debug/user-info/{username}")
public ResponseEntity<Map<String, Object>> getUserInfo(@PathVariable String username) {
    try {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(username);
        }
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map<String, Object> info = new HashMap<>();
            info.put("userId", user.getId());
            info.put("username", user.getUsername());
            info.put("email", user.getEmail());
            info.put("passwordLength", user.getPassword() != null ? user.getPassword().length() : 0);
            info.put("passwordFormat", user.getPassword() != null ? 
                user.getPassword().substring(0, Math.min(20, user.getPassword().length())) + "..." : "NULL");
            info.put("hasPrefix", user.getPassword() != null && user.getPassword().startsWith("{bcrypt}"));
            info.put("createdAt", user.getCreatedAt());
            
            return ResponseEntity.ok(info);
        } else {
            return ResponseEntity.notFound().build();
        }
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}
@PostMapping("/debug/reset-user-password")
public ResponseEntity<?> resetUserPassword(@RequestBody Map<String, String> request) {
    try {
        String username = request.get("username");
        String newPassword = request.get("newPassword");
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByEmail(username);
        }
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Encode the new password with proper prefix
            String encodedPassword = encoder.encode(newPassword);
            user.setPassword(encodedPassword);
            userRepository.save(user);
            
            logger.info("✅ Password reset for user: {}", username);
            
            return ResponseEntity.ok(Map.of(
                "message", "Password reset successfully",
                "username", username,
                "newPasswordFormat", encodedPassword.substring(0, 20) + "..."
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    } catch (Exception e) {
        logger.error("Error resetting password", e);
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}


}
