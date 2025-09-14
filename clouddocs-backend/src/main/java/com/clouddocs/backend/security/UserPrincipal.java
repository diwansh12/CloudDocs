package com.clouddocs.backend.security;

import com.clouddocs.backend.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;
import java.util.stream.Collectors;

public class UserPrincipal implements UserDetails {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;
    
    public UserPrincipal(Long id, String username, String email, String firstName, String lastName, 
                        String password, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
        this.authorities = authorities;
    }
    
    // ✅ FIXED: Updated for Many-to-Many Role system
    public static UserPrincipal create(User user) {
        // ✅ FIXED: Use ArrayList instead of List.of() to avoid type issues
        List<GrantedAuthority> authorities = new ArrayList<>();
        
        // ✅ FIXED: Handle Many-to-Many roles - user.getRoles() returns Set<Role>
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .collect(Collectors.toList());
        } else {
            // ✅ Fallback: If no roles, add default USER role
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        
        return new UserPrincipal(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getPassword(),
            authorities
        );
    }
    
    // Getters
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    
    @Override
    public String getUsername() { return username; }
    
    @Override
    public String getPassword() { return password; }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    
    @Override
    public boolean isAccountNonExpired() { return true; }
    
    @Override
    public boolean isAccountNonLocked() { return true; }
    
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    
    @Override
    public boolean isEnabled() { return true; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
