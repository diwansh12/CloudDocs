package com.clouddocs.backend.repository;

import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.Role;
import com.clouddocs.backend.entity.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // ✅ BASIC QUERIES
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

      // ✅ ADDED: Find users by role using Many-to-Many relationship
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r = :role AND u.active = :active AND u.enabled = :enabled")
    List<User> findByRoleAndActiveAndEnabled(@Param("role") Role role, @Param("active") Boolean active, @Param("enabled") Boolean enabled);
    

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
List<User> findByRolesContaining(@Param("role") Role role);


    // ✅ CRITICAL: Combined query for login (username OR email)
    @Query("SELECT u FROM User u WHERE u.username = :usernameOrEmail OR u.email = :usernameOrEmail")
    Optional<User> findByUsernameOrEmail(@Param("usernameOrEmail") String usernameOrEmail, @Param("usernameOrEmail") String usernameOrEmail2);
    
    // ✅ EXISTENCE CHECKS
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    // ✅ MANY-TO-MANY: Role-based queries with JOIN
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findByRole(@Param("role") Role role);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r IN :roles")
    List<User> findByRolesIn(@Param("roles") Set<Role> roles);
    
    // ✅ ROLE NAME QUERIES: More practical for enum-based roles
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName")
    List<User> findByRoleName(@Param("roleName") ERole roleName);
    
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name IN :roleNames")
    List<User> findByRoleNameIn(@Param("roleNames") Set<ERole> roleNames);

    // ✅ ADMIN QUERIES: Users with specific role combinations
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.active = :active AND u.enabled = :enabled")
    List<User> findByRoleNameAndActiveAndEnabled(@Param("roleName") ERole roleName, @Param("active") boolean active, @Param("enabled") boolean enabled);

    // ✅ COUNT QUERIES for statistics
    long countByActiveTrue();
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    long countByRoleName(@Param("roleName") ERole roleName);
    
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name IN :roleNames")
    long countByRoleNameIn(@Param("roleNames") Set<ERole> roleNames);

    // ✅ PASSWORD MIGRATION SUPPORT
    @Query("SELECT COUNT(u) FROM User u WHERE u.password LIKE :prefix%")
    long countByPasswordStartingWith(@Param("prefix") String prefix);
    
    @Query("SELECT u FROM User u WHERE u.password NOT LIKE '$2a$%' AND u.password NOT LIKE '$2b$%'")
    List<User> findUsersWithPlaintextPasswords();

    // ✅ ADMIN MANAGEMENT QUERIES
    @Query("SELECT u FROM User u WHERE u.createdAt IS NULL")
    List<User> findByCreatedAtIsNull();
    
    @Query("SELECT u FROM User u WHERE u.lastLogin IS NOT NULL ORDER BY u.lastLogin DESC")
    List<User> findRecentlyActiveUsers();

    // ✅ SEARCH QUERIES
    @Query("SELECT u FROM User u WHERE u.firstName LIKE %:query% OR u.lastName LIKE %:query% OR u.username LIKE %:query% OR u.email LIKE %:query%")
    List<User> searchUsers(@Param("query") String query);
    
    // ✅ ADDITIONAL MANY-TO-MANY SPECIFIC QUERIES
    
    // Find users who have ALL specified roles (AND condition)
    @Query("SELECT u FROM User u WHERE (SELECT COUNT(r) FROM u.roles r WHERE r.name IN :roleNames) = :roleCount")
    List<User> findUsersWithAllRoles(@Param("roleNames") Set<ERole> roleNames, @Param("roleCount") long roleCount);
    
    // Find users who have ANY of the specified roles (OR condition) - same as findByRoleNameIn
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name IN :roleNames")
    List<User> findUsersWithAnyRole(@Param("roleNames") Set<ERole> roleNames);
    
    // Find users who DON'T have a specific role
    @Query("SELECT u FROM User u WHERE u.id NOT IN (SELECT DISTINCT u2.id FROM User u2 JOIN u2.roles r WHERE r.name = :roleName)")
    List<User> findUsersWithoutRole(@Param("roleName") ERole roleName);
    
    // Count users by multiple roles
    @Query("SELECT r.name, COUNT(DISTINCT u) FROM User u JOIN u.roles r GROUP BY r.name")
    List<Object[]> countUsersByEachRole();
    
    // Find users with exactly N roles
    @Query("SELECT u FROM User u WHERE SIZE(u.roles) = :roleCount")
    List<User> findUsersWithExactRoleCount(@Param("roleCount") int roleCount);
    
    // Find users with more than N roles
    @Query("SELECT u FROM User u WHERE SIZE(u.roles) > :roleCount")
    List<User> findUsersWithMoreThanRoles(@Param("roleCount") int roleCount);
    
    // Find users by role and date range
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.createdAt BETWEEN :startDate AND :endDate")
    List<User> findByRoleNameAndCreatedAtBetween(@Param("roleName") ERole roleName, @Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);
    
    // Find recently active users with specific role
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND u.lastLogin IS NOT NULL ORDER BY u.lastLogin DESC")
    List<User> findRecentlyActiveUsersByRole(@Param("roleName") ERole roleName);
    
    // Advanced search with role filter
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.name = :roleName AND (u.firstName LIKE %:query% OR u.lastName LIKE %:query% OR u.username LIKE %:query% OR u.email LIKE %:query%)")
    List<User> searchUsersByRole(@Param("query") String query, @Param("roleName") ERole roleName);
}
