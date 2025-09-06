package com.clouddocs.backend.service;

import com.clouddocs.backend.dto.UserProfileDTO;
import com.clouddocs.backend.dto.UserProfileUpdateRequest;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.Role;
import com.clouddocs.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserProfileDTO getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        return convertToDTO(user);
    }

    // ✅ In your UserService.uploadProfilePicture method
public UserProfileDTO uploadProfilePicture(String username, MultipartFile file) throws IOException {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found"));
    
    // ✅ Store file in profile-pictures subfolder
    String fileName = fileStorageService.storeFile(file, "profile-pictures");
    
    // ✅ Save the relative path in database
    user.setProfilePicture(fileName);
    userRepository.save(user);
    
    return convertToDTO(user);
}


    public UserProfileDTO updateProfile(String username, UserProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setLastModified(LocalDateTime.now());
        
        user = userRepository.save(user);
        return convertToDTO(user);
    }

    public void changePassword(String username, String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void logLogoutActivity(String username) {
        // Log logout activity - implement as needed
        System.out.println("User " + username + " logged out at " + LocalDateTime.now());
    }

    public List<UserProfileDTO> getAllUsers(int page, int size) {
        // Implement pagination as needed
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .toList();
    }

    public UserProfileDTO updateUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setRole(Role.valueOf(role.toUpperCase()));
        user = userRepository.save(user);
        
        return convertToDTO(user);
    }

    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        user.setActive(false);
        userRepository.save(user);
    }

    public Map<String, Object> getUserStatistics() {
        // Implement user statistics
        return Map.of(
            "totalUsers", userRepository.count(),
            "activeUsers", userRepository.countByActiveTrue()
        );
    }

    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    public List<Object> getUserActivityLog(String username, int page, int size) {
        // Implement activity log retrieval
        return List.of(); // Placeholder
    }

    public UserProfileDTO deleteProfilePicture(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (user.getProfilePicture() != null) {
            // Delete file from storage
            fileStorageService.deleteFile("profile-pictures/" + user.getProfilePicture());
            user.setProfilePicture(null);
            user = userRepository.save(user);
        }
        
        return convertToDTO(user);
    }

    private UserProfileDTO convertToDTO(User user) {
        return new UserProfileDTO(
            user.getId(),
            user.getFullName(),
            user.getUsername(),
            user.getEmail(),
            user.getRole().name(),
            user.getProfilePicture(),
            user.getCreatedAt(),
            user.getLastLoginAt(),
            user.isActive()
        );
    }
}
