package com.clouddocs.backend.repository;

import com.clouddocs.backend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * ✅ FIXED: Updated to use user_name column instead of user
     */
    @Query(value = "SELECT * FROM audit_log a WHERE " +
           "(:q IS NULL OR " +
           " (a.activity IS NOT NULL AND LOWER(a.activity) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
           " (a.linked_item IS NOT NULL AND LOWER(a.linked_item) LIKE LOWER(CONCAT('%', :q, '%')))" +
           ") " +
           "AND (:user IS NULL OR " +
           "     (a.user_name IS NOT NULL AND LOWER(a.user_name) = LOWER(:user))" +  // ✅ CHANGED user to user_name
           ") " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:from IS NULL OR a.timestamp >= :from) " +
           "AND (:to IS NULL OR a.timestamp <= :to) " +
           "ORDER BY a.timestamp DESC", nativeQuery = true)
    List<AuditLog> search(
            @Param("q")      String    q,
            @Param("user")   String    user,
            @Param("status") String    status,
            @Param("from")   LocalDate from,
            @Param("to")     LocalDate to
    );

    /**
     * ✅ SAFER: Use JPA method names (automatically use correct column mapping)
     */
    List<AuditLog> findByLinkedItemOrderByTimestampDesc(String linkedItem);
    
    List<AuditLog> findByUserOrderByTimestampDesc(String user);
    
    List<AuditLog> findByActivityContainingIgnoreCaseOrderByTimestampDesc(String activity);
    
    List<AuditLog> findByStatusOrderByTimestampDesc(AuditLog.Status status);
}
