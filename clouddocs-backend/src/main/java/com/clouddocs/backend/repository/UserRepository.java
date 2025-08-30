package com.clouddocs.backend.repository;

import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    List<User> findByRole(Role role); // No @Query needed

    List<User> findByRoleIn(List<Role> roles);

    long countByActiveTrue();
}

