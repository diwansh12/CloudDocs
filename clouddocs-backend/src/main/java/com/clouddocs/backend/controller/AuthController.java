package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.request.LoginRequest;
import com.clouddocs.backend.dto.request.SignupRequest;
import com.clouddocs.backend.dto.response.JwtResponse;
import com.clouddocs.backend.dto.response.MessageResponse;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.Role;
import com.clouddocs.backend.entity.ERole;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.repository.RoleRepository;
import com.clouddocs.backend.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"}, 
             maxAge = 3600, allowCredentials = "true")
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository; // ✅ ADDED: RoleRepository for Many-to-Many
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * ✅ ENHANCED: Login endpoint with better error handling
     */
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
            
            // Check for null/empty values
            if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty() || 
                loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Username and password are required"));
            }
            
            // Debug logging
            logger.debug("Username: {}, Password length: {}", 
                loginRequest.getUsername(), 
                loginRequest.getPassword() != null ? loginRequest.getPassword().length() : "null");
            
            // ✅ ENHANCED: Use AuthService for consistent authentication
            JwtResponse jwtResponse = authService.authenticate(loginRequest);
            
            logger.info("✅ Login successful for user: {} with roles: {}", 
                loginRequest.getUsername(), jwtResponse.getRoles());
            return ResponseEntity.ok(jwtResponse);
            
        } catch (RuntimeException e) {
            logger.error("❌ Login failed for user: {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("Invalid username or password"));
        } catch (Exception e) {
            logger.error("❌ Unexpected login error for user: {}", loginRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Authentication service temporarily unavailable"));
        }
    }
    
    /**
     * ✅ FIXED: Registration endpoint with Many-to-Many role support
     */
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest,
                                        BindingResult bindingResult) {
        try {
            logger.info("📝 Registration request for: {} with roles: {}", 
                signUpRequest.getUsername(), signUpRequest.getRole());
            
            // Validate input
            if (bindingResult.hasErrors()) {
                logger.warn("⚠️ Validation errors for registration: {}", signUpRequest.getUsername());
                List<String> errors = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.toList());
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Validation errors: " + String.join(", ", errors)));
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
            
            // ✅ FIXED: Create user with Many-to-Many roles
            User user = new User();
            user.setUsername(signUpRequest.getUsername());
            user.setEmail(signUpRequest.getEmail());
            user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
            user.setFirstName(signUpRequest.getFirstName());
            user.setLastName(signUpRequest.getLastName());
            user.setActive(true);
            user.setEnabled(true);
            
            // ✅ FIXED: Assign roles using Many-to-Many mapping
            Set<Role> roles = assignRoles(signUpRequest.getRole());
            user.setRoles(roles);
            
            User savedUser = userRepository.save(user);
            
            List<String> roleNames = savedUser.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());
            
            logger.info("✅ User registered successfully: ID={}, Username={}, Email={}, Roles={}", 
                savedUser.getId(), savedUser.getUsername(), savedUser.getEmail(), roleNames);
            
            // ✅ ENHANCED: Auto-login after registration
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername(signUpRequest.getUsername());
            loginRequest.setPassword(signUpRequest.getPassword());
            
            JwtResponse jwtResponse = authService.authenticate(loginRequest);
            
            return ResponseEntity.ok(jwtResponse);
            
        } catch (RuntimeException e) {
            logger.error("❌ Registration failed for user: {}", signUpRequest.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Registration failed: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("❌ Unexpected registration error for user: {}", signUpRequest.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Registration service temporarily unavailable"));
        }
    }
    
    /**
     * ✅ NEW: Role assignment logic for registration
     */
    private Set<Role> assignRoles(Set<String> strRoles) {
        Set<Role> roles = new HashSet<>();
        
        if (strRoles == null || strRoles.isEmpty()) {
            // Default role assignment
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Default USER role not found."));
            roles.add(userRole);
            logger.info("🔧 Assigned default USER role");
        } else {
            // Custom role assignment
            for (String roleName : strRoles) {
                try {
                    // Normalize role name (add ROLE_ prefix if missing)
                    String normalizedRoleName = roleName.startsWith("ROLE_") 
                        ? roleName.toUpperCase() 
                        : "ROLE_" + roleName.toUpperCase();
                    
                    ERole eRole = ERole.valueOf(normalizedRoleName);
                    Role role = roleRepository.findByName(eRole)
                        .orElseThrow(() -> new RuntimeException("Error: Role " + roleName + " not found."));
                    roles.add(role);
                    logger.info("🔧 Assigned role: {}", eRole);
                    
                } catch (IllegalArgumentException e) {
                    logger.warn("⚠️ Invalid role name: {}. Skipping...", roleName);
                    // Continue processing other roles instead of failing
                }
            }
            
            // Fallback: If no valid roles were found, assign USER role
            if (roles.isEmpty()) {
                Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Default USER role not found."));
                roles.add(userRole);
                logger.info("🔧 No valid roles found, assigned default USER role");
            }
        }
        
        return roles;
    }
    
    /**
     * ✅ ENHANCED: Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("authService", authService != null ? "INJECTED" : "NULL");
        status.put("userRepository", userRepository != null ? "INJECTED" : "NULL");
        status.put("roleRepository", roleRepository != null ? "INJECTED" : "NULL");
        status.put("passwordEncoder", passwordEncoder != null ? passwordEncoder.getClass().getSimpleName() : "NULL");
        status.put("userCount", userRepository.count());
        status.put("roleCount", roleRepository.count());
        status.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(status);
    }
    
    /**
 * ✅ FIXED: Debug endpoint with role information - type mismatch resolved
 */
@GetMapping("/debug")
public ResponseEntity<Map<String, Object>> debug() {
    Map<String, Object> info = new HashMap<>();
    info.put("userCount", userRepository.count());
    info.put("roleCount", roleRepository.count());
    
    // ✅ FIXED: List all available roles using HashMap instead of Map.of()
    List<Map<String, Object>> availableRoles = roleRepository.findAll().stream()
        .map(role -> {
            Map<String, Object> roleMap = new HashMap<>();
            roleMap.put("id", role.getId());
            roleMap.put("name", role.getName().name());
            roleMap.put("description", role.getDescription() != null ? role.getDescription() : "No description");
            return roleMap;
        })
        .collect(Collectors.toList());
    info.put("availableRoles", availableRoles);
    
    // ✅ FIXED: List first 5 users with their roles using HashMap
    List<Map<String, Object>> sampleUsers = userRepository.findAll().stream()
        .limit(5)
        .map(user -> {
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("username", user.getUsername());
            userInfo.put("email", user.getEmail());
            userInfo.put("firstName", user.getFirstName());
            userInfo.put("lastName", user.getLastName());
            userInfo.put("active", user.isActive());
            userInfo.put("enabled", user.isEnabled());
            userInfo.put("roles", user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList()));
            return userInfo;
        })
        .collect(Collectors.toList());
        
    info.put("sampleUsers", sampleUsers);
    return ResponseEntity.ok(info);
}


    /**
     * ✅ UPDATED: Debug user endpoint with role information
     */
    @GetMapping("/debug/user/{username}")
    public ResponseEntity<Map<String, Object>> debugUser(@PathVariable String username) {
        try {
            Map<String, Object> info = new HashMap<>();
            
            Optional<User> userOpt = userRepository.findByUsernameOrEmail(username, username);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                info.put("userExists", true);
                info.put("userId", user.getId());
                info.put("username", user.getUsername());
                info.put("email", user.getEmail());
                info.put("firstName", user.getFirstName());
                info.put("lastName", user.getLastName());
                info.put("active", user.isActive());
                info.put("enabled", user.isEnabled());
                info.put("roles", user.getRoles().stream()
                    .map(role -> Map.of(
                        "id", role.getId(),
                        "name", role.getName().name(),
                        "description", role.getDescription()
                    ))
                    .collect(Collectors.toList()));
                
                // Password analysis
                String passwordPreview = user.getPassword() != null ? 
                    user.getPassword().substring(0, Math.min(20, user.getPassword().length())) + "..." : "NULL";
                info.put("passwordFormat", passwordPreview);
                info.put("passwordLength", user.getPassword() != null ? user.getPassword().length() : 0);
                info.put("isBcryptEncoded", user.getPassword() != null && 
                    user.getPassword().matches("^\\$2[abxy]\\$\\d+\\$.*"));
                info.put("createdAt", user.getCreatedAt());
                info.put("lastLogin", user.getLastLogin());
                
            } else {
                info.put("userExists", false);
                info.put("totalUsers", userRepository.count());
                info.put("searchedFor", username);
            }
            
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Test role assignment
     */
    @PostMapping("/debug/test-roles")
    public ResponseEntity<?> testRoleAssignment(@RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            Set<String> roleNames = new HashSet<>((List<String>) request.get("roles"));
            
            Set<Role> assignedRoles = assignRoles(roleNames);
            
            Map<String, Object> result = new HashMap<>();
            result.put("inputRoles", roleNames);
            result.put("assignedRoles", assignedRoles.stream()
                .map(role -> Map.of(
                    "id", role.getId(),
                    "name", role.getName().name(),
                    "description", role.getDescription()
                ))
                .collect(Collectors.toList()));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ ENHANCED: Test password encoding
     */
    @PostMapping("/debug/test-password")
    public ResponseEntity<Map<String, Object>> testPassword(@RequestBody Map<String, String> request) {
        try {
            String rawPassword = request.get("password");
            String encodedPassword = passwordEncoder.encode(rawPassword);
            
            Map<String, Object> result = new HashMap<>();
            result.put("rawPassword", rawPassword);
            result.put("encodedPassword", encodedPassword);
            result.put("encoderType", passwordEncoder.getClass().getSimpleName());
            result.put("matchesItself", passwordEncoder.matches(rawPassword, encodedPassword));
            result.put("isBcryptFormat", encodedPassword.matches("^\\$2[abxy]\\$\\d+\\$.*"));
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Add role to user
     */
    @PostMapping("/debug/add-role")
    public ResponseEntity<?> addRoleToUser(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String roleName = request.get("role");
            
            Optional<User> userOpt = userRepository.findByUsernameOrEmail(username, username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            // Normalize role name
            String normalizedRoleName = roleName.startsWith("ROLE_") 
                ? roleName.toUpperCase() 
                : "ROLE_" + roleName.toUpperCase();
            
            ERole eRole = ERole.valueOf(normalizedRoleName);
            Role role = roleRepository.findByName(eRole)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
            
            User user = userOpt.get();
            user.addRole(role);
            userRepository.save(user);
            
            return ResponseEntity.ok(Map.of(
                "message", "Role added successfully",
                "username", username,
                "addedRole", eRole.name(),
                "currentRoles", user.getRoles().stream()
                    .map(r -> r.getName().name())
                    .collect(Collectors.toList())
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ FIXED: Reset user password with proper encoding
     */
    @PostMapping("/debug/reset-user-password")
    public ResponseEntity<?> resetUserPassword(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String newPassword = request.get("newPassword");
            
            Optional<User> userOpt = userRepository.findByUsernameOrEmail(username, username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            User user = userOpt.get();
            String encodedPassword = passwordEncoder.encode(newPassword);
            user.setPassword(encodedPassword);
            userRepository.save(user);
            
            logger.info("✅ Password reset for user: {}", username);
            
            return ResponseEntity.ok(Map.of(
                "message", "Password reset successfully",
                "username", username,
                "newPasswordFormat", encodedPassword.substring(0, 20) + "...",
                "isBcryptFormat", encodedPassword.matches("^\\$2[abxy]\\$\\d+\\$.*")
            ));
            
        } catch (Exception e) {
            logger.error("Error resetting password", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ NEW: Get user statistics
     */
    @GetMapping("/debug/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            
            stats.put("totalUsers", userRepository.count());
            stats.put("activeUsers", userRepository.countByActiveTrue());
            
            // Count users by role
            Map<String, Long> roleStats = new HashMap<>();
            for (ERole eRole : ERole.values()) {
                long count = userRepository.countByRoleName(eRole);
                roleStats.put(eRole.name(), count);
            }
            stats.put("usersByRole", roleStats);
            
            // Password migration stats
            long encodedPasswords = userRepository.countByPasswordStartingWith("$2");
            long totalUsers = userRepository.count();
            stats.put("passwordMigrationProgress", totalUsers > 0 ? 
                (double) encodedPasswords / totalUsers * 100 : 0);
            
            stats.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
