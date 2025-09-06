package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.UserProfileDTO;
import com.clouddocs.backend.dto.UserProfileUpdateRequest;
import com.clouddocs.backend.dto.ChangePasswordRequest;
import com.clouddocs.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

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

   

    /**
     * Get current authenticated user's profile
     */

     
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
 * ✅ FIXED: Controller-based image serving with proper security check
 */
@GetMapping("/profile/picture/{path}/{filename:.+}")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Resource> getProfilePicture(
        @PathVariable String path,
        @PathVariable String filename) {
    try {
        System.out.println("🖼️ Serving image: " + path + "/" + filename);
        
        // ✅ FIXED: Proper path resolution and security check
        Path uploadPath = Paths.get("./uploads").toAbsolutePath().normalize();
        Path imagePath = uploadPath.resolve(path).resolve(filename).normalize();
        
        System.out.println("🔍 Upload path: " + uploadPath);
        System.out.println("🔍 Image path: " + imagePath);
        System.out.println("🔍 Starts with check: " + imagePath.startsWith(uploadPath));
        
        // ✅ CORRECTED: Security check with proper path normalization
        if (!imagePath.normalize().startsWith(uploadPath.normalize())) {
            System.err.println("❌ Security violation: Path traversal attempt blocked");
            System.err.println("    Requested path: " + imagePath);
            System.err.println("    Base path: " + uploadPath);
            return ResponseEntity.notFound().build();
        }
        
        // ✅ Additional security: Only allow 'profile-pictures' subfolder
        if (!"profile-pictures".equals(path)) {
            System.err.println("❌ Invalid subfolder: " + path);
            return ResponseEntity.notFound().build();
        }
        
        if (!Files.exists(imagePath)) {
            System.err.println("❌ Image not found: " + imagePath);
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = new UrlResource(imagePath.toUri());
        
        // ✅ Determine content type
        String contentType = Files.probeContentType(imagePath);
        if (contentType == null) {
            // Fallback content type detection
            String lowerFilename = filename.toLowerCase();
            if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (lowerFilename.endsWith(".png")) {
                contentType = "image/png";
            } else if (lowerFilename.endsWith(".webp")) {
                contentType = "image/webp";
            } else if (lowerFilename.endsWith(".gif")) {
                contentType = "image/gif";
            } else {
                contentType = "application/octet-stream";
            }
        }
        
        System.out.println("✅ Successfully serving: " + path + "/" + filename + " as " + contentType);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
                
    } catch (Exception e) {
        System.err.println("❌ Error serving image: " + e.getMessage());
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

    /**
 * ✅ Debug endpoint to check uploads directory
 */
@GetMapping("/debug/uploads")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> debugUploads() {
    Map<String, Object> info = new HashMap<>();
    
    try {
        Path uploadPath = Paths.get("./uploads").toAbsolutePath().normalize();
        Path profilePicturesPath = uploadPath.resolve("profile-pictures");
        
        info.put("uploadPath", uploadPath.toString());
        info.put("profilePicturesPath", profilePicturesPath.toString());
        info.put("uploadExists", Files.exists(uploadPath));
        info.put("profilePicturesExists", Files.exists(profilePicturesPath));
        
        if (Files.exists(profilePicturesPath)) {
            List<String> files = Files.list(profilePicturesPath)
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toList());
            info.put("files", files);
            info.put("fileCount", files.size());
        }
        
    } catch (Exception e) {
        info.put("error", e.getMessage());
    }
    
    return ResponseEntity.ok(info);
}

}
