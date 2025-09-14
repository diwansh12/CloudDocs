package com.clouddocs.backend.repository;

import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ✅ BASIC QUERIES
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    
    // ✅ CRITICAL: Combined query for login (username OR email)
    @Query("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail, @Param("usernameOrEmail") String usernameOrEmail2);
    
    // ✅ EXISTENCE CHECKS
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    // ✅ FIXED: Role-based queries (assuming User has Set<Role> roles)
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRole(@Param("role") Role role);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r IN :roles")
    List<User> findByRoleIn(@Param("roles") List<Role> roles);
    
    // ✅ NEW: Find users by role name (more practical)
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name IN :roleNames")
    List<User> findByRoleNameIn(@Param("roleNames") List<String> roleNames);

    // ✅ ENHANCED: User statistics and admin queries
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.active = :active AND u.enabled = :enabled")
    List<User> findByRoleAndActiveAndEnabled(@Param("role") Role role, @Param("active") boolean active, @Param("enabled") boolean enabled);

    // ✅ NEW: Count queries for statistics
    long countByActiveTrue();
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    long countByRoleName(@Param("roleName") String roleName);
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name IN :roleNames")
    long countByRoleNameIn(@Param("roleNames") List<String> roleNames);

    // ✅ NEW: Password migration support
    @Query("SELECT COUNT(u) FROM User u WHERE u.password LIKE :prefix%")
    long countByPasswordStartingWith(@Param("prefix") String prefix);
    
    @Query("SELECT u FROM User u WHERE u.password NOT LIKE '$2a$%' AND u.password NOT LIKE '$2b$%'")
    List<User> findUsersWithPlaintextPasswords();

    // ✅ NEW: Admin management queries
    @Query("SELECT u FROM User u WHERE u.createdAt IS NULL")
    List<User> findByCreatedAtIsNull();
    
    @Query("SELECT u FROM User u WHERE u.lastLoginAt IS NOT NULL ORDER BY u.lastLoginAt DESC")
    List<User> findRecentlyActiveUsers();

    // ✅ NEW: Search queries
    @Query("SELECT u FROM User u WHERE u.firstName LIKE %:query% OR u.lastName LIKE %:query% OR u.username LIKE %:query% OR u.email LIKE %:query%")
    List<User> searchUsers(@Param("query") String query);
}
