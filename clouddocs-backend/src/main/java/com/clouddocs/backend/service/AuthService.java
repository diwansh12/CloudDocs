package com.clouddocs.backend.service;

import com.clouddocs.backend.dto.request.LoginRequest;
import com.clouddocs.backend.dto.request.SignupRequest; // ✅ FIXED: Using SignupRequest consistently
import com.clouddocs.backend.dto.response.JwtResponse;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.Role;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.security.JwtTokenProvider;
import com.clouddocs.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * ✅ ENHANCED: Authenticate user with proper password encoding support
     */
    public JwtResponse authenticate(LoginRequest loginRequest) {
        try {
            logger.info("🔐 Authentication attempt for user: {}", loginRequest.getUsername());
            
            // Check if user exists first
            Optional<User> userOptional = userRepository.findByUsernameOrEmail(
                loginRequest.getUsername(), 
                loginRequest.getUsername()
            );
            
            if (userOptional.isEmpty()) {
                logger.warn("⚠️ User not found: {}", loginRequest.getUsername());
                throw new BadCredentialsException("Invalid username or password");
            }
            
            User user = userOptional.get();
            
            // ✅ AUTO-MIGRATION: Handle legacy plain text passwords
            if (!isPasswordEncoded(user.getPassword())) {
                logger.warn("🔄 Found unencoded password for user: {}. Auto-migrating...", user.getUsername());
                if (loginRequest.getPassword().equals(user.getPassword())) {
                    String encodedPassword = passwordEncoder.encode(loginRequest.getPassword());
                    user.setPassword(encodedPassword);
                    userRepository.save(user);
                    logger.info("✅ Auto-migrated password for user: {}", user.getUsername());
                }
            }
            
            // Authenticate with Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(), 
                    loginRequest.getPassword()
                )
            );
            
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            String jwt = jwtTokenProvider.generateJwtToken(authentication);
            
            List<String> roles = userPrincipal.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            
            logger.info("✅ Authentication successful for user: {}", userPrincipal.getUsername());
            
            return new JwtResponse(
                jwt,
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getEmail(),
                userPrincipal.getFirstName(),
                userPrincipal.getLastName(),
                roles
            );
            
        } catch (BadCredentialsException e) {
            logger.error("❌ Invalid credentials for user: {}", loginRequest.getUsername());
            throw new BadCredentialsException("Invalid username or password");
        } catch (Exception e) {
            logger.error("❌ Authentication failed for user: {}", loginRequest.getUsername(), e);
            throw new RuntimeException("Authentication failed: " + e.getMessage());
        }
    }
    
    /**
     * ✅ FIXED: User registration with SignupRequest and manual User creation
     */
    @Transactional
    public JwtResponse register(SignupRequest signupRequest) { // ✅ FIXED: Changed from RegisterRequest to SignupRequest
        try {
            logger.info("📝 Registration attempt for user: {}", signupRequest.getUsername());
            
            // Check if username already exists
            if (userRepository.existsByUsername(signupRequest.getUsername())) {
                logger.warn("⚠️ Username already exists: {}", signupRequest.getUsername());
                throw new RuntimeException("Username is already taken");
            }
            
            // Check if email already exists
            if (userRepository.existsByEmail(signupRequest.getEmail())) {
                logger.warn("⚠️ Email already exists: {}", signupRequest.getEmail());
                throw new RuntimeException("Email is already in use");
            }
            
            // ✅ CRITICAL: Encode password before saving
            String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());
            
            // ✅ FIXED: Create new user manually without builder pattern
            User user = new User();
            user.setUsername(signupRequest.getUsername());
            user.setEmail(signupRequest.getEmail());
            user.setPassword(encodedPassword);
            user.setFirstName(signupRequest.getFirstName());
            user.setLastName(signupRequest.getLastName());
            
            // ✅ FIXED: Set default role - handle role assignment
            if (signupRequest.getRole() != null && !signupRequest.getRole().isEmpty()) {
                // If roles are provided, use the first one or handle multiple roles
                String firstRole = signupRequest.getRole().iterator().next().toUpperCase();
                try {
                    user.setRole(Role.valueOf(firstRole));
                } catch (IllegalArgumentException e) {
                    logger.warn("⚠️ Invalid role '{}', assigning USER role", firstRole);
                    user.setRole(Role.USER);
                }
            } else {
                user.setRole(Role.USER); // Default role
            }
            
            // Set additional defaults
            user.setActive(true);
            user.setEnabled(true);
            
            User savedUser = userRepository.save(user);
            logger.info("✅ User registered successfully: {}", savedUser.getUsername());
            
            // Automatically log in the user after registration
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(signupRequest.getUsername());
            loginRequest.setPassword(signupRequest.getPassword());
            
            return authenticate(loginRequest);
            
        } catch (DataIntegrityViolationException e) {
            logger.error("❌ Registration failed - data integrity violation: {}", e.getMessage());
            throw new RuntimeException("Username or email already exists");
        } catch (Exception e) {
            logger.error("❌ Registration failed for user: {}", signupRequest.getUsername(), e);
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }
    
    /**
     * ✅ NEW: Change password with proper encoding
     */
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        try {
            logger.info("🔄 Password change request for user: {}", username);
            
            User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Verify current password
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                logger.warn("⚠️ Invalid current password for user: {}", username);
                throw new BadCredentialsException("Current password is incorrect");
            }
            
            // Encode and save new password
            String encodedNewPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedNewPassword);
            userRepository.save(user);
            
            logger.info("✅ Password changed successfully for user: {}", username);
            
        } catch (Exception e) {
            logger.error("❌ Password change failed for user: {}", username, e);
            throw new RuntimeException("Password change failed: " + e.getMessage());
        }
    }
    
    /**
     * ✅ ENHANCED: Check if user exists by username or email
     */
    public boolean userExists(String usernameOrEmail) {
        return userRepository.existsByUsername(usernameOrEmail) || 
               userRepository.existsByEmail(usernameOrEmail);
    }
    
    /**
     * ✅ NEW: Get user by username or email
     */
    public Optional<User> findUserByUsernameOrEmail(String usernameOrEmail) {
        return userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail);
    }
    
    /**
     * ✅ NEW: Password validation utility
     */
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
    
    /**
     * ✅ NEW: Check if password is already encoded (for migration support)
     */
    private boolean isPasswordEncoded(String password) {
        // BCrypt passwords start with $2a$, $2b$, $2x$, or $2y$
        return password != null && password.matches("^\\$2[abxy]\\$\\d+\\$.*");
    }
    
    /**
     * ✅ NEW: Encode plain password (utility method)
     */
    public String encodePassword(String plainPassword) {
        return passwordEncoder.encode(plainPassword);
    }
    
    /**
     * ✅ NEW: Get user count for debugging/admin purposes
     */
    public long getUserCount() {
        return userRepository.count();
    }
    
    /**
     * ✅ FIXED: Get user statistics with proper repository method check
     */
    public UserStats getUserStats() {
        long totalUsers = userRepository.count();
        
        // ✅ SAFE: Check if countByPasswordStartingWith method exists
        long encodedPasswords = 0;
        try {
            encodedPasswords = userRepository.countByPasswordStartingWith("$2");
        } catch (Exception e) {
            logger.warn("⚠️ countByPasswordStartingWith method not available, calculating manually");
            // Alternative calculation if method doesn't exist
            List<User> allUsers = userRepository.findAll();
            encodedPasswords = allUsers.stream()
                .mapToLong(user -> isPasswordEncoded(user.getPassword()) ? 1 : 0)
                .sum();
        }
        
        long plainPasswords = totalUsers - encodedPasswords;
        
        return UserStats.builder()
            .totalUsers(totalUsers)
            .usersWithEncodedPasswords(encodedPasswords)
            .usersWithPlainPasswords(plainPasswords)
            .migrationProgress(totalUsers > 0 ? (double) encodedPasswords / totalUsers * 100 : 0)
            .build();
    }
    
    /**
     * ✅ NEW: Batch password migration support
     */
    @Transactional
    public int migratePasswordsForUsers(List<Long> userIds) {
        int migratedCount = 0;
        
        for (Long userId : userIds) {
            Optional<User> userOptional = userRepository.findById(userId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                
                if (!isPasswordEncoded(user.getPassword())) {
                    String encodedPassword = passwordEncoder.encode(user.getPassword());
                    user.setPassword(encodedPassword);
                    userRepository.save(user);
                    migratedCount++;
                    
                    logger.debug("✅ Migrated password for user: {}", user.getUsername());
                }
            }
        }
        
        logger.info("🎉 Batch password migration completed: {} users migrated", migratedCount);
        return migratedCount;
    }
    
    /**
     * ✅ NEW: User statistics inner class
     */
    public static class UserStats {
        private final long totalUsers;
        private final long usersWithEncodedPasswords;
        private final long usersWithPlainPasswords;
        private final double migrationProgress;
        
        private UserStats(Builder builder) {
            this.totalUsers = builder.totalUsers;
            this.usersWithEncodedPasswords = builder.usersWithEncodedPasswords;
            this.usersWithPlainPasswords = builder.usersWithPlainPasswords;
            this.migrationProgress = builder.migrationProgress;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public long getTotalUsers() { return totalUsers; }
        public long getUsersWithEncodedPasswords() { return usersWithEncodedPasswords; }
        public long getUsersWithPlainPasswords() { return usersWithPlainPasswords; }
        public double getMigrationProgress() { return migrationProgress; }
        
        public static class Builder {
            private long totalUsers;
            private long usersWithEncodedPasswords;
            private long usersWithPlainPasswords;
            private double migrationProgress;
            
            public Builder totalUsers(long totalUsers) {
                this.totalUsers = totalUsers;
                return this;
            }
            
            public Builder usersWithEncodedPasswords(long usersWithEncodedPasswords) {
                this.usersWithEncodedPasswords = usersWithEncodedPasswords;
                return this;
            }
            
            public Builder usersWithPlainPasswords(long usersWithPlainPasswords) {
                this.usersWithPlainPasswords = usersWithPlainPasswords;
                return this;
            }
            
            public Builder migrationProgress(double migrationProgress) {
                this.migrationProgress = migrationProgress;
                return this;
            }
            
            public UserStats build() {
                return new UserStats(this);
            }
        }
    }
}
