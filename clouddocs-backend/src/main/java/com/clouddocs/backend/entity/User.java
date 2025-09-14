package com.clouddocs.backend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String username;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone_number")
    private String phoneNumber;
    
    // ✅ CHANGED: Many-to-Many relationship with roles
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @UpdateTimestamp
    @Column(name = "last_modified")
    private LocalDateTime lastModified;
    
    @Column(name = "profile_picture")
    private String profilePicture;
    
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
    
    // Constructors
    public User() {}
    
    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.enabled = true;
        this.active = true;
        this.roles = new HashSet<>();
    }
    
    public User(String username, String email, String password, String firstName, String lastName) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = true;
        this.active = true;
        this.roles = new HashSet<>();
    }
    
    // ✅ UPDATED: UserDetails implementation for multiple roles
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
            .map(role -> new SimpleGrantedAuthority(role.getName().name()))
            .collect(Collectors.toSet());
    }
    
    @Override
    public boolean isAccountNonExpired() { return true; }
    
    @Override
    public boolean isAccountNonLocked() { return true; }
    
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    
    @Override
    public boolean isEnabled() { 
        return enabled != null ? enabled : true; 
    }
    
    // ✅ NEW: Roles management
    public Set<Role> getRoles() { 
        return roles; 
    }
    
    public void setRoles(Set<Role> roles) { 
        this.roles = roles != null ? roles : new HashSet<>(); 
    }
    
    // ✅ UTILITY: Add/remove roles
    public void addRole(Role role) {
        if (role != null) {
            this.roles.add(role);
        }
    }
    
    public void removeRole(Role role) {
        this.roles.remove(role);
    }
    
    public boolean hasRole(ERole roleName) {
        return roles.stream().anyMatch(role -> role.getName() == roleName);
    }
    
    // ✅ BACKWARD COMPATIBILITY: Get primary role
    public ERole getPrimaryRole() {
        return roles.isEmpty() ? ERole.ROLE_USER : roles.iterator().next().getName();
    }
    
    // All existing getters and setters remain the same
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getLastLogin() { return lastLogin; }
    public void setLastLogin(LocalDateTime lastLogin) { this.lastLogin = lastLogin; }
    
    public LocalDateTime getLastModified() { return lastModified; }
    public void setModified(LocalDateTime lastModified) { this.lastModified = lastModified; }
    
    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String profilePicture) { this.profilePicture = profilePicture; }
    
    public Boolean isActive() { 
        return active != null ? active : true; 
    }
    
    public void setActive(Boolean active) { 
        this.active = active != null ? active : true; 
    }
    
    public Boolean getEnabled() { 
        return enabled != null ? enabled : true; 
    }
    
    public void setEnabled(Boolean enabled) { 
        this.enabled = enabled != null ? enabled : true; 
    }
    
    // Utility methods
    public String getFullName() {
        if (firstName == null && lastName == null) return username;
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }
    
    public void setFullName(String fullName) {
        if (fullName != null && !fullName.trim().isEmpty()) {
            String[] parts = fullName.trim().split(" ", 2);
            this.firstName = parts[0];
            this.lastName = parts.length > 1 ? parts[1] : "";
        }
    }
    
    public LocalDateTime getLastLoginAt() {
        return this.lastLogin;
    }
}
