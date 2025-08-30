package com.clouddocs.backend.repository;

import com.clouddocs.backend.entity.User;
import com.clouddocs.backend.entity.UserNotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserNotificationSettingsRepository extends JpaRepository<UserNotificationSettings, Long> {
    
    Optional<UserNotificationSettings> findByUser(User user);
    
    Optional<UserNotificationSettings> findByUserId(Long userId);
    
    void deleteByUser(User user);
    
    boolean existsByUser(User user);
}
