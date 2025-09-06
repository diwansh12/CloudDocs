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
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByRoleIn(List<Role> roles);

    
List<User> findByCreatedAtIsNull();


  
     @Query("SELECT u FROM User u WHERE u.role = :role AND u.active = :active AND u.enabled = :enabled")
     List<User> findByRoleAndActiveAndEnabled(@Param("role") Role role, @Param("active") boolean active, @Param("enabled") boolean enabled);

    long countByActiveTrue();
}

