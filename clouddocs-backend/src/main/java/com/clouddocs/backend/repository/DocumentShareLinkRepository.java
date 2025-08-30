package com.clouddocs.backend.repository;

import com.clouddocs.backend.entity.Document;
import com.clouddocs.backend.entity.DocumentShareLink;
import com.clouddocs.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for DocumentShareLink entity
 */
@Repository
public interface DocumentShareLinkRepository extends JpaRepository<DocumentShareLink, Long> {

    // ===== BASIC FIND OPERATIONS =====

    /**
     * Find all active share links for a specific document
     */
    List<DocumentShareLink> findByDocumentAndActiveTrue(Document document);

    /**
     * Find active share link by share ID
     */
    Optional<DocumentShareLink> findByShareIdAndActiveTrue(String shareId);

    /**
     * Find share link by share ID (including inactive)
     */
    Optional<DocumentShareLink> findByShareId(String shareId);

    /**
     * Find all share links for a document (including inactive)
     */
    List<DocumentShareLink> findByDocumentOrderByCreatedAtDesc(Document document);

    /**
     * Find share links created by a specific user
     */
    List<DocumentShareLink> findByCreatedByAndActiveTrueOrderByCreatedAtDesc(User createdBy);

    /**
     * Find share links for documents uploaded by a specific user
     */
    @Query("SELECT sl FROM DocumentShareLink sl WHERE sl.document.uploadedBy = :user AND sl.active = true ORDER BY sl.createdAt DESC")
    List<DocumentShareLink> findByDocumentOwnerAndActiveTrue(@Param("user") User user);

    // ===== PAGINATED QUERIES =====

    /**
     * Find all active share links with pagination
     */
    Page<DocumentShareLink> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find share links for a specific document with pagination
     */
    Page<DocumentShareLink> findByDocumentOrderByCreatedAtDesc(Document document, Pageable pageable);

    /**
     * Find share links created by a user with pagination
     */
    Page<DocumentShareLink> findByCreatedByOrderByCreatedAtDesc(User createdBy, Pageable pageable);

    // ===== EXPIRATION AND CLEANUP =====

    /**
     * Find all expired share links
     */
    @Query("SELECT sl FROM DocumentShareLink sl WHERE sl.expiresAt IS NOT NULL AND sl.expiresAt < :now AND sl.active = true")
    List<DocumentShareLink> findExpiredActiveLinks(@Param("now") LocalDateTime now);

    /**
     * Find share links expiring soon (within specified hours)
     */
    @Query("SELECT sl FROM DocumentShareLink sl WHERE sl.expiresAt IS NOT NULL AND sl.expiresAt BETWEEN :now AND :threshold AND sl.active = true")
    List<DocumentShareLink> findLinksExpiringSoon(@Param("now") LocalDateTime now, @Param("threshold") LocalDateTime threshold);

    /**
     * Deactivate expired share links
     */
    @Modifying
    @Transactional
    @Query("UPDATE DocumentShareLink sl SET sl.active = false, sl.updatedAt = :now WHERE sl.expiresAt IS NOT NULL AND sl.expiresAt < :now AND sl.active = true")
    int deactivateExpiredLinks(@Param("now") LocalDateTime now);

    /**
     * Delete share links that expired before the specified date
     */
    @Modifying
    @Transactional
    void deleteByExpiresAtBeforeAndActiveFalse(LocalDateTime dateTime);

    // ===== ACCESS TRACKING =====

    /**
     * Find most accessed share links
     */
    @Query("SELECT sl FROM DocumentShareLink sl WHERE sl.active = true ORDER BY sl.accessCount DESC")
    List<DocumentShareLink> findMostAccessedLinks(Pageable pageable);

    /**
     * Find share links with access count greater than specified value
     */
    List<DocumentShareLink> findByAccessCountGreaterThanAndActiveTrueOrderByAccessCountDesc(Integer minAccessCount);

    /**
     * Find share links accessed after a specific date
     */
    List<DocumentShareLink> findByLastAccessedAtAfterAndActiveTrueOrderByLastAccessedAtDesc(LocalDateTime date);

    // ===== STATISTICS QUERIES =====

    /**
     * Count active share links for a document
     */
    long countByDocumentAndActiveTrue(Document document);

    /**
     * Count share links created by a user
     */
    long countByCreatedByAndActiveTrue(User createdBy);

    /**
     * Count total active share links
     */
    long countByActiveTrue();

    /**
     * Count expired active links
     */
    @Query("SELECT COUNT(sl) FROM DocumentShareLink sl WHERE sl.expiresAt IS NOT NULL AND sl.expiresAt < :now AND sl.active = true")
    long countExpiredActiveLinks(@Param("now") LocalDateTime now);

    /**
     * Get total access count for all share links
     */
    @Query("SELECT COALESCE(SUM(sl.accessCount), 0) FROM DocumentShareLink sl WHERE sl.active = true")
    long getTotalAccessCount();

    /**
     * Get statistics for share links created in date range
     */
    @Query("SELECT COUNT(sl), COALESCE(SUM(sl.accessCount), 0) FROM DocumentShareLink sl WHERE sl.createdAt BETWEEN :startDate AND :endDate")
    Object[] getStatisticsForDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // ===== PASSWORD AND SECURITY =====

    /**
     * Find password-protected share links
     */
    @Query("SELECT sl FROM DocumentShareLink sl WHERE sl.password IS NOT NULL AND sl.active = true")
    List<DocumentShareLink> findPasswordProtectedLinks();

    /**
     * Find share links that allow download
     */
    List<DocumentShareLink> findByAllowDownloadTrueAndActiveTrueOrderByCreatedAtDesc();

    /**
     * Find share links that don't allow download (view only)
     */
    List<DocumentShareLink> findByAllowDownloadFalseAndActiveTrueOrderByCreatedAtDesc();

    // ===== ADVANCED QUERIES =====

    /**
     * Find share links with maximum access count reached
     */
    @Query("SELECT sl FROM DocumentShareLink sl WHERE sl.maxAccessCount IS NOT NULL AND sl.accessCount >= sl.maxAccessCount AND sl.active = true")
    List<DocumentShareLink> findLinksWithMaxAccessReached();

    /**
     * Find share links for documents of specific status
     */
    @Query("SELECT sl FROM DocumentShareLink sl WHERE sl.document.status = :status AND sl.active = true ORDER BY sl.createdAt DESC")
    List<DocumentShareLink> findByDocumentStatus(@Param("status") com.clouddocs.backend.entity.DocumentStatus status);

    /**
     * Find share links for documents in specific category
     */
    @Query("SELECT sl FROM DocumentShareLink sl WHERE sl.document.category = :category AND sl.active = true ORDER BY sl.createdAt DESC")
    List<DocumentShareLink> findByDocumentCategory(@Param("category") String category);

    /**
     * Search share links by document name or share ID
     */
    @Query("SELECT sl FROM DocumentShareLink sl WHERE (sl.document.originalFilename LIKE %:searchTerm% OR sl.shareId LIKE %:searchTerm%) AND sl.active = true ORDER BY sl.createdAt DESC")
    Page<DocumentShareLink> searchShareLinks(@Param("searchTerm") String searchTerm, Pageable pageable);

    // ===== BATCH OPERATIONS =====

    /**
     * Find share links by multiple share IDs
     */
    List<DocumentShareLink> findByShareIdInAndActiveTrue(List<String> shareIds);

    /**
     * Revoke multiple share links
     */
    @Modifying
    @Transactional
    @Query("UPDATE DocumentShareLink sl SET sl.active = false, sl.revokedAt = :revokedAt, sl.revokedBy = :revokedBy WHERE sl.shareId IN :shareIds AND sl.active = true")
    int revokeMultipleLinks(@Param("shareIds") List<String> shareIds, @Param("revokedAt") LocalDateTime revokedAt, @Param("revokedBy") User revokedBy);

    /**
     * Deactivate all share links for a document
     */
    @Modifying
    @Transactional
    @Query("UPDATE DocumentShareLink sl SET sl.active = false, sl.revokedAt = :revokedAt, sl.revokedBy = :revokedBy WHERE sl.document = :document AND sl.active = true")
    int revokeAllLinksForDocument(@Param("document") Document document, @Param("revokedAt") LocalDateTime revokedAt, @Param("revokedBy") User revokedBy);

    // ===== MAINTENANCE QUERIES =====

    /**
     * Find share links older than specified days that can be cleaned up
     */
    @Query("SELECT sl FROM DocumentShareLink sl WHERE sl.active = false AND sl.revokedAt IS NOT NULL AND sl.revokedAt < :cutoffDate")
    List<DocumentShareLink> findOldRevokedLinks(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Get share link usage report
     */
    @Query("SELECT sl.document.category, COUNT(sl), COALESCE(SUM(sl.accessCount), 0) FROM DocumentShareLink sl WHERE sl.active = true GROUP BY sl.document.category ORDER BY COUNT(sl) DESC")
    List<Object[]> getUsageReportByCategory();

    /**
     * Get daily share link creation stats for the last N days
     */
    @Query("SELECT DATE(sl.createdAt), COUNT(sl) FROM DocumentShareLink sl WHERE sl.createdAt >= :startDate GROUP BY DATE(sl.createdAt) ORDER BY DATE(sl.createdAt)")
    List<Object[]> getDailyCreationStats(@Param("startDate") LocalDateTime startDate);

    // ===== CUSTOM VALIDATION QUERIES =====

    /**
     * Check if a share ID already exists
     */
    boolean existsByShareId(String shareId);

    /**
     * Check if user has active share links for a document
     */
    @Query("SELECT CASE WHEN COUNT(sl) > 0 THEN true ELSE false END FROM DocumentShareLink sl WHERE sl.document = :document AND sl.createdBy = :user AND sl.active = true")
    boolean hasActiveShareLinksForDocument(@Param("document") Document document, @Param("user") User user);

    /**
     * Count active share links for a user within time period
     */
    @Query("SELECT COUNT(sl) FROM DocumentShareLink sl WHERE sl.createdBy = :user AND sl.active = true AND sl.createdAt >= :since")
    long countActiveShareLinksForUserSince(@Param("user") User user, @Param("since") LocalDateTime since);
}
