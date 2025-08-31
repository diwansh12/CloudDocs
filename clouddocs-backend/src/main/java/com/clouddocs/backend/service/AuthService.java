package com.clouddocs.backend.service;

import com.clouddocs.backend.dto.request.LoginRequest;
import com.clouddocs.backend.dto.response.JwtResponse;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.security.JwtTokenProvider;
import com.clouddocs.backend.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.List;
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
    
    public JwtResponse authenticate(LoginRequest loginRequest) {
        try {
            logger.info("🔐 Authentication attempt for user: {}", loginRequest.getUsername());
            
            // Check if user exists first
            boolean userExistsByUsername = userRepository.existsByUsername(loginRequest.getUsername());
            boolean userExistsByEmail = userRepository.existsByEmail(loginRequest.getUsername());
            
            if (!userExistsByUsername && !userExistsByEmail) {
                logger.warn("⚠️ User not found: {}", loginRequest.getUsername());
                throw new BadCredentialsException("Invalid username or password");
            }
            
            // Authenticate user credentials
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getUsername(), 
                    loginRequest.getPassword()
                )
            );
            
            // Extract user principal
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            
            // Generate JWT token
            String jwt = jwtTokenProvider.generateJwtToken(authentication);
            
            // Extract roles
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
     * Check if user exists by username or email
     */
    public boolean userExists(String usernameOrEmail) {
        return userRepository.existsByUsername(usernameOrEmail) || 
               userRepository.existsByEmail(usernameOrEmail);
    }
    
    /**
     * Get user count for debugging
     */
    public long getUserCount() {
        return userRepository.count();
    }
}
