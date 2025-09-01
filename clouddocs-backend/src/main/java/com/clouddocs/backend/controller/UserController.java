package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.UserProfileDTO;
import com.clouddocs.backend.dto.UserProfileUpdateRequest;
import com.clouddocs.backend.dto.ChangePasswordRequest;
import com.clouddocs.backend.service.UserService;
import com.clouddocs.backend.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.cache.Cache;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * REST Controller for User Profile Management
 * Handles user profile operations, profile pictures, and user settings
 */
@RestController
@RequestMapping("/users")
@CrossOrigin(origins = {"https://cloud-docs-tan.vercel.app", "http://localhost:3000"})
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
private JdbcTemplate jdbcTemplate;

@Autowired
private EntityManager entityManager;

@Autowired(required = false)
private CacheManager cacheManager;

    /**
     * Get current authenticated user's profile
     */

     @GetMapping("/db-status")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getDatabaseStatus() {
    Map<String, Object> status = new HashMap<>();
    
    try {
        // Check database connection
        String dbTime = jdbcTemplate.queryForObject("SELECT NOW()", String.class);
        status.put("databaseTime", dbTime);
        status.put("connectionStatus", "CONNECTED");
        
        // Check current database name
        String dbName = jdbcTemplate.queryForObject(
            "SELECT current_database()", String.class);
        status.put("databaseName", dbName);
        
        // Count users in database
        Long userCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users", Long.class);
        status.put("totalUsersInDatabase", userCount);
        
        // Get actual user data from database (direct SQL)
        List<Map<String, Object>> users = jdbcTemplate.queryForList(
            "SELECT id, username, email, role, enabled, active FROM users ORDER BY id LIMIT 10"
        );
        status.put("actualUsersInDatabase", users);
        
        // Check what Spring repository returns
        List<UserProfileDTO> springUsers = userService.getAllUsers(0, 10);
        status.put("springRepositoryUserCount", springUsers.size());
        status.put("springRepositoryUsers", springUsers);
        
        // Get current authenticated user details
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) auth.getPrincipal();
            status.put("currentUsername", userDetails.getUsername());
            status.put("currentAuthorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toList()));
        }
        
        return ResponseEntity.ok(status);
        
    } catch (Exception e) {
        status.put("error", e.getMessage());
        status.put("connectionStatus", "FAILED");
        return ResponseEntity.status(500).body(status);
    }
}

/**
 * ✅ PRODUCTION CACHE CLEARING: Clear all caches safely
 */
@PostMapping("/clear-caches")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> clearProductionCaches() {
    Map<String, Object> result = new HashMap<>();
    
    try {
        // Clear JPA/Hibernate first-level cache
        entityManager.clear();
        result.put("jpaCache", "cleared");
        
        // Clear Spring caches if present
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
            result.put("springCaches", "cleared");
        } else {
            result.put("springCaches", "not configured");
        }
        
        // Clear security context
        SecurityContextHolder.clearContext();
        result.put("securityContext", "cleared");
        
        result.put("status", "success");
        result.put("message", "All caches cleared successfully");
        return ResponseEntity.ok(result);
        
    } catch (Exception e) {
        result.put("error", e.getMessage());
        result.put("status", "failed");
        return ResponseEntity.status(500).body(result);
    }
}

/**
 * ✅ CURRENT USER DEBUG: Check what Spring Security sees for current user
 */
@GetMapping("/debug-current-user")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> debugCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
    Map<String, Object> debug = new HashMap<>();
    
    try {
        // Current authentication info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        debug.put("authenticationClass", auth.getClass().getSimpleName());
        debug.put("principalClass", auth.getPrincipal().getClass().getSimpleName());
        debug.put("authoritiesFromAuth", auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toList()));
        
        // UserDetails info
        debug.put("userDetailsUsername", userDetails.getUsername());
        debug.put("userDetailsAuthorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toList()));
        
        // Fresh database data
        try {
            UserProfileDTO dbUser = userService.getProfileByUsername(userDetails.getUsername());
            debug.put("dbUserRole", dbUser.getRole());
            debug.put("dbUserEmail", dbUser.getEmail());
            debug.put("dbUserActive", dbUser.isActive());
        } catch (Exception e) {
            debug.put("dbError", e.getMessage());
        }
        
        return ResponseEntity.ok(debug);
        
    } catch (Exception e) {
        debug.put("error", e.getMessage());
        return ResponseEntity.status(500).body(debug);
    }
}

/**
 * ✅ FORCE REFRESH: Force reload user from database
 */
@PostMapping("/force-refresh")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> forceRefreshUserData(@AuthenticationPrincipal UserDetails userDetails) {
    Map<String, Object> result = new HashMap<>();
    
    try {
        // Clear entity manager to force fresh database query
        entityManager.clear();
        
        // Get fresh user data
        UserProfileDTO freshProfile = userService.getProfileByUsername(userDetails.getUsername());
        
        result.put("message", "User data refreshed successfully");
        result.put("freshUserData", freshProfile);
        result.put("currentAuthorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toList()));
        
        return ResponseEntity.ok(result);
        
    } catch (Exception e) {
        result.put("error", e.getMessage());
        return ResponseEntity.status(500).body(result);
    }
}

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCurrentUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            if (userDetails == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not authenticated");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            UserProfileDTO profile = userService.getProfileByUsername(userDetails.getUsername());
            return ResponseEntity.ok(profile);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch user profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * ✅ ENHANCED: Upload user profile picture with better validation
     */
    @PostMapping(value = "/profile/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadProfilePicture(
            @RequestParam("profilePicture") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            // Enhanced validation
            if (file.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Please select a file to upload");
                return ResponseEntity.badRequest().body(error);
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Only image files are allowed (JPEG, PNG, GIF, WebP)");
                return ResponseEntity.badRequest().body(error);
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "File size must be less than 5MB");
                return ResponseEntity.badRequest().body(error);
            }

            // ✅ DEBUG: Log upload details
            System.out.println("📸 Uploading profile picture for user: " + userDetails.getUsername());
            System.out.println("📸 File name: " + file.getOriginalFilename());
            System.out.println("📸 File size: " + file.getSize() + " bytes");
            System.out.println("📸 Content type: " + contentType);

            UserProfileDTO updatedProfile = userService.uploadProfilePicture(userDetails.getUsername(), file);
            
            // ✅ DEBUG: Log saved path
            System.out.println("✅ Profile picture saved: " + updatedProfile.getProfilePicture());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile picture updated successfully");
            response.put("user", updatedProfile);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Profile picture upload failed: " + e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload profile picture: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * ✅ ENHANCED: Serve user profile pictures with better content type handling
     */
    @GetMapping("/profile/picture/{filename:.+}")
public ResponseEntity<?> getProfilePicture(@PathVariable String filename) {
    try {
        System.out.println("🖼️ Attempting to serve image: " + filename);
        
        // ✅ CRITICAL: Load resource correctly based on how you store files
        Resource resource = fileStorageService.loadFileAsResource(filename);
        
        if (!resource.exists() || !resource.isReadable()) {
            System.err.println("❌ Image file not found or not readable: " + filename);
            return ResponseEntity.notFound().build();
        }
        
        // Determine content type
        String contentType = "image/jpeg";
        String lowerFilename = filename.toLowerCase();
        
        if (lowerFilename.endsWith(".png")) {
            contentType = "image/png";
        } else if (lowerFilename.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (lowerFilename.endsWith(".webp")) {
            contentType = "image/webp";
        }
        
        System.out.println("✅ Successfully serving image: " + filename + " as " + contentType);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache") // Disable cache for testing
                .body(resource);
                
    } catch (Exception e) {
        System.err.println("❌ Failed to serve image: " + filename + " - " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.notFound().build();
    }
}
    /**
     * Update user profile information
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody UserProfileUpdateRequest updateRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            UserProfileDTO updatedProfile = userService.updateProfile(userDetails.getUsername(), updateRequest);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("user", updatedProfile);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update profile: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Change user password
     */
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest changePasswordRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        try {
            userService.changePassword(
                userDetails.getUsername(), 
                changePasswordRequest.getCurrentPassword(), 
                changePasswordRequest.getNewPassword()
            );
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password changed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to change password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * ✅ NEW: Delete user profile picture
     */
    @DeleteMapping("/profile/picture")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteProfilePicture(@AuthenticationPrincipal UserDetails userDetails) {
        try {
            UserProfileDTO updatedProfile = userService.deleteProfilePicture(userDetails.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile picture deleted successfully");
            response.put("user", updatedProfile);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to delete profile picture: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get all users (Admin only)
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            List<UserProfileDTO> users = userService.getAllUsers(page, size);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch users: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Update user role (Admin only)
     */
    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long userId,
            @RequestParam String role) {
        
        try {
            UserProfileDTO updatedUser = userService.updateUserRole(userId, role);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User role updated successfully");
            response.put("user", updatedUser);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update user role: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Deactivate user account (Admin only)
     */
    @PutMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateUser(@PathVariable Long userId) {
        try {
            userService.deactivateUser(userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "User account deactivated successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to deactivate user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    /**
     * Get user statistics (Admin/Manager only)
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> getUserStatistics() {
        try {
            Map<String, Object> stats = userService.getUserStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch user statistics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * User logout
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth != null) {
                userService.logLogoutActivity(auth.getName());
            }

            if (request.getSession(false) != null) {
                request.getSession().invalidate();
            }

            SecurityContextHolder.clearContext();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Logged out successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Logout failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Check if username is available
     */
    @GetMapping("/check-username")
    public ResponseEntity<?> checkUsernameAvailability(@RequestParam String username) {
        try {
            boolean isAvailable = userService.isUsernameAvailable(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("available", isAvailable);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to check username availability: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Get current user's activity log
     */
    @GetMapping("/activity-log")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getUserActivityLog(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            List<Object> activityLog = userService.getUserActivityLog(userDetails.getUsername(), page, size);
            return ResponseEntity.ok(activityLog);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch activity log: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
