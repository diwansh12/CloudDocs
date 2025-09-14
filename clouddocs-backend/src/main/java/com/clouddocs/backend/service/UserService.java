package com.clouddocs.backend.service;

import com.clouddocs.backend.dto.UserProfileDTO;
import com.clouddocs.backend.dto.UserProfileUpdateRequest;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.Role;
import com.clouddocs.backend.entity.ERole;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository; // ✅ ADDED: For Many-to-Many role management

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public UserProfileDTO getProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        return convertToDTO(user);
    }

    // ✅ Profile picture upload method
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

    // ✅ FIXED: Update profile with correct setter method
    public UserProfileDTO updateProfile(String username, UserProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        
        // ✅ FIXED: Use setModified() instead of setLastModified()
        user.setModified(LocalDateTime.now());
        
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

    // ✅ FIXED: Update user role with Many-to-Many system
    public UserProfileDTO updateUserRole(Long userId, String roleStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        
        // ✅ FIXED: Convert string to ERole enum and find Role entity
        try {
            String enumName = roleStr.startsWith("ROLE_") ? roleStr : "ROLE_" + roleStr;
            ERole eRole = ERole.valueOf(enumName.toUpperCase());
            
            Role roleEntity = roleRepository.findByName(eRole)
                .orElseThrow(() -> new RuntimeException("Role not found: " + enumName));
            
            // ✅ FIXED: Set roles using Many-to-Many system
            Set<Role> userRoles = new HashSet<>();
            userRoles.add(roleEntity);
            user.setRoles(userRoles);
            
            user = userRepository.save(user);
            
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + roleStr);
        }
        
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

    // ✅ FIXED: Convert to DTO with Many-to-Many role system
    private UserProfileDTO convertToDTO(User user) {
        // ✅ FIXED: Get role name from Many-to-Many roles
        String roleName = "USER"; // Default
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            roleName = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.joining(", "));
        }
        
        return new UserProfileDTO(
            user.getId(),
            user.getFullName(),
            user.getUsername(),
            user.getEmail(),
            roleName,
            user.getProfilePicture(),
            user.getCreatedAt(),
            user.getLastLoginAt(),
            user.isActive()
        );
    }

    // ✅ NEW: Add user with specific roles
    public UserProfileDTO addUserWithRoles(String username, String email, String password, List<String> roleNames) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setActive(true);
        user.setEnabled(true);

        // ✅ Set roles using Many-to-Many system
        Set<Role> userRoles = new HashSet<>();
        for (String roleName : roleNames) {
            try {
                String enumName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
                ERole eRole = ERole.valueOf(enumName.toUpperCase());
                
                Role roleEntity = roleRepository.findByName(eRole)
                    .orElse(null);
                
                if (roleEntity != null) {
                    userRoles.add(roleEntity);
                }
            } catch (IllegalArgumentException e) {
                // Log warning but continue with other roles
                System.out.println("Invalid role skipped: " + roleName);
            }
        }

        // ✅ If no valid roles found, add default USER role
        if (userRoles.isEmpty()) {
            Role defaultRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElse(null);
            if (defaultRole != null) {
                userRoles.add(defaultRole);
            }
        }

        user.setRoles(userRoles);
        user = userRepository.save(user);

        return convertToDTO(user);
    }

    // ✅ NEW: Get users by role
    public List<UserProfileDTO> getUsersByRole(String roleName) {
        try {
            String enumName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
            ERole eRole = ERole.valueOf(enumName.toUpperCase());
            
            Role roleEntity = roleRepository.findByName(eRole)
                .orElse(null);
            
            if (roleEntity != null) {
                List<User> users = userRepository.findByRolesContaining(roleEntity);
                return users.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            }
        } catch (IllegalArgumentException e) {
            // Invalid role name
        }
        
        return new ArrayList<>();
    }

    // ✅ NEW: Add role to user
    public UserProfileDTO addRoleToUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        try {
            String enumName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
            ERole eRole = ERole.valueOf(enumName.toUpperCase());
            
            Role roleEntity = roleRepository.findByName(eRole)
                .orElseThrow(() -> new RuntimeException("Role not found: " + enumName));
            
            user.addRole(roleEntity);
            user = userRepository.save(user);
            
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + roleName);
        }

        return convertToDTO(user);
    }

    // ✅ NEW: Remove role from user
    public UserProfileDTO removeRoleFromUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        try {
            String enumName = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
            ERole eRole = ERole.valueOf(enumName.toUpperCase());
            
            Role roleEntity = roleRepository.findByName(eRole)
                .orElse(null);
            
            if (roleEntity != null) {
                user.removeRole(roleEntity);
                user = userRepository.save(user);
            }
            
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role: " + roleName);
        }

        return convertToDTO(user);
    }
}
