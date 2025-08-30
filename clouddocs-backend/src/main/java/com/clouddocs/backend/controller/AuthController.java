package com.clouddocs.backend.controller;

import com.clouddocs.backend.dto.request.LoginRequest;
import com.clouddocs.backend.dto.request.SignupRequest;
import com.clouddocs.backend.dto.response.JwtResponse;
import com.clouddocs.backend.dto.response.MessageResponse;
import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.repository.UserRepository;
import com.clouddocs.backend.security.JwtTokenProvider;
import com.clouddocs.backend.security.UserPrincipal; // ✅ Fixed import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, BindingResult bindingResult) {
        // 🔥 Add validation error checking
        if (bindingResult.hasErrors()) {
            System.err.println("=== VALIDATION ERRORS ===");
            bindingResult.getAllErrors().forEach(error -> {
                System.err.println("Validation error: " + error.getDefaultMessage());
            });
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Validation errors: " + bindingResult.getAllErrors()));
        }

        try {
            System.out.println("=== LOGIN ATTEMPT ===");
            System.out.println("Username: " + loginRequest.getUsername());
            System.out.println("Password length: " + (loginRequest.getPassword() != null ? loginRequest.getPassword().length() : "null"));
            
            // 🔥 Debug: Check if user exists in database
            System.out.println("Checking if user exists...");
            boolean userExistsByUsername = userRepository.existsByUsername(loginRequest.getUsername());
            boolean userExistsByEmail = userRepository.existsByEmail(loginRequest.getUsername());
            System.out.println("User exists by username: " + userExistsByUsername);
            System.out.println("User exists by email: " + userExistsByEmail);
            
            if (!userExistsByUsername && !userExistsByEmail) {
                System.out.println("=== USER NOT FOUND - Listing all users ===");
                List<User> allUsers = userRepository.findAll();
                allUsers.forEach(u -> System.out.println("- ID: " + u.getId() + ", Username: " + u.getUsername() + ", Email: " + u.getEmail()));
            }
            
            Authentication authentication = authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(), 
                            loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtTokenProvider.generateJwtToken(authentication);

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            
            List<String> roles = userPrincipal.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            System.out.println("✅ Login successful for: " + userPrincipal.getUsername());

            return ResponseEntity.ok(new JwtResponse(jwt,
                    userPrincipal.getId(),
                    userPrincipal.getUsername(),
                    userPrincipal.getEmail(),
                    userPrincipal.getFirstName(),
                    userPrincipal.getLastName(),
                    roles));
        } catch (Exception e) {
            System.err.println("❌ Login failed for user: " + loginRequest.getUsername());
            System.err.println("Error type: " + e.getClass().getSimpleName());
            System.err.println("Error message: " + e.getMessage());
            e.printStackTrace();
            
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error: Invalid username or password!"));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        try {
            if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Username is already taken!"));
            }

            if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(new MessageResponse("Error: Email is already in use!"));
            }

            User user = new User(signUpRequest.getUsername(),
                               signUpRequest.getEmail(),
                               encoder.encode(signUpRequest.getPassword()));

            user.setFirstName(signUpRequest.getFirstName());
            user.setLastName(signUpRequest.getLastName());
            user.setCreatedAt(LocalDateTime.now());
            user.setEnabled(true);
            user.setRole(com.clouddocs.backend.entity.Role.USER);

            User savedUser = userRepository.save(user);
            System.out.println("✅ User registered: ID=" + savedUser.getId() + ", Username=" + savedUser.getUsername() + ", Email=" + savedUser.getEmail());

            return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(new MessageResponse("Error: " + e.getMessage()));
        }
    }
}

