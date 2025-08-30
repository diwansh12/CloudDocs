package com.clouddocs.backend.repository;

import com.clouddocs.backend.entity.Notification;
import com.clouddocs.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);

    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Notification> findByUserAndReadFlagFalseOrderByCreatedAtDesc(User user);

    Page<Notification> findByUserAndReadFlagFalseOrderByCreatedAtDesc(User user, Pageable pageable);

    long countByUserAndReadFlagFalse(User user);
}

