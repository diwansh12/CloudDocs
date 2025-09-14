package com.clouddocs.backend.service;

import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("🔍 CustomUserDetailsService: Looking for user: " + username);
        
        // Try to find user by username first
        Optional<User> userOpt = userRepository.findByUsername(username);
        System.out.println("User found by username: " + userOpt.isPresent());
        
        // If not found by username, try by email
        if (userOpt.isEmpty()) {
            System.out.println("User not found by username, trying email...");
            userOpt = userRepository.findByEmail(username);
            System.out.println("User found by email: " + userOpt.isPresent());
        }
        
        User user = userOpt.orElseThrow(() -> {
            System.out.println("❌ User not found anywhere: " + username);
            
            // Debug: List all users in database
            List<User> allUsers = userRepository.findAll();
            System.out.println("All users in database:");
            allUsers.forEach(u -> System.out.println("- Username: " + u.getUsername() + ", Email: " + u.getEmail()));
            
            return new UsernameNotFoundException("User Not Found: " + username);
        });
        
        System.out.println("✅ User found: " + user.getUsername() + " (" + user.getEmail() + ")");
        System.out.println("User enabled: " + user.isEnabled());
        
        // ✅ FIXED: Use getRoles() for Many-to-Many system and format for display
        String userRoles = user.getRoles().stream()
            .map(role -> role.getName().name())
            .collect(Collectors.joining(", "));
        System.out.println("User roles: [" + userRoles + "]");

        return UserPrincipal.create(user); // ✅ Using UserPrincipal instead of UserDetailsImpl
    }
}
