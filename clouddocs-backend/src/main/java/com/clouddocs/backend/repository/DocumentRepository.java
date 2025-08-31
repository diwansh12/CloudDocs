package com.clouddocs.backend.repository;

import com.clouddocs.backend.entity.Document;
import com.clouddocs.backend.entity.DocumentStatus;
import com.clouddocs.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // ===== BASIC FIND METHODS =====
    
    // Find documents by status
    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);

    // Find documents by uploaded user
    Page<Document> findByUploadedBy(User user, Pageable pageable);

    // Find documents by uploaded user ID
    Page<Document> findByUploadedById(Long uploadedById, Pageable pageable);

    // ✅ CRITICAL: JOIN FETCH to prevent lazy loading exceptions
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.id = :userId ORDER BY d.uploadDate DESC")
    Page<Document> findByUploadedByIdOrderByUploadDateDesc(@Param("userId") Long userId, Pageable pageable);

    // Find documents by status and user
    Page<Document> findByStatusAndUploadedBy(DocumentStatus status, User user, Pageable pageable);

    // Find documents by category
    Page<Document> findByCategory(String category, Pageable pageable);

    // Find documents by document type
    Page<Document> findByDocumentType(String documentType, Pageable pageable);

    // ===== SEARCH AND FILTER METHODS WITH JOIN FETCH =====
    
    // ✅ CRITICAL: Search with JOIN FETCH to avoid lazy loading
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE " +
           "LOWER(d.filename) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Document> searchDocuments(@Param("query") String query, Pageable pageable);

    // ✅ CRITICAL: Find all with JOIN FETCH
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy")
    Page<Document> findAllWithUsers(Pageable pageable);

    // ✅ CRITICAL: Date range with JOIN FETCH
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadDate BETWEEN :startDate AND :endDate")
    Page<Document> findByUploadDateBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate, 
                                         Pageable pageable);

    // ✅ CRITICAL: Pending documents with JOIN FETCH
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.status = 'PENDING'")
    Page<Document> findPendingDocuments(Pageable pageable);

    // ✅ CRITICAL: Recent documents with JOIN FETCH
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "ORDER BY d.uploadDate DESC")
    Page<Document> findRecentDocuments(Pageable pageable);

    // ✅ Advanced filter with JOIN FETCH to prevent lazy loading
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE " +
           "(:query IS NULL OR :query = '' OR " +
           "LOWER(d.filename) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:status IS NULL OR d.status = :status) AND " +
           "(:category IS NULL OR :category = '' OR d.category = :category) AND " +
           "(:documentType IS NULL OR :documentType = '' OR d.documentType = :documentType) AND " +
           "(:uploadedBy IS NULL OR d.uploadedBy = :uploadedBy)")
    Page<Document> findWithFilters(@Param("query") String query,
                                 @Param("status") DocumentStatus status,
                                 @Param("category") String category,
                                 @Param("documentType") String documentType,
                                 @Param("uploadedBy") User uploadedBy,
                                 Pageable pageable);

    // ===== COUNT METHODS =====
    
    // Count documents by status
    long countByStatus(DocumentStatus status);

    // Count documents by user
    long countByUploadedBy(User user);

    // Count documents by user ID
    long countByUploadedById(Long uploadedById);

    // Count documents by user ID and status
    long countByUploadedByIdAndStatus(Long uploadedById, DocumentStatus status);

    // Count documents uploaded after specific date
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadDate > :date")
    long countByUploadDateAfter(@Param("date") LocalDateTime date);

    // ===== TAGS AND CATEGORIES =====
    
    // Find documents by tags
    @Query("SELECT DISTINCT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "JOIN d.tags t WHERE t IN :tags")
    Page<Document> findByTagsIn(@Param("tags") List<String> tags, Pageable pageable);

    // Get all unique categories
    @Query("SELECT DISTINCT d.category FROM Document d WHERE d.category IS NOT NULL ORDER BY d.category")
    List<String> findAllCategories();

    // Get all unique tags
    @Query("SELECT DISTINCT t FROM Document d JOIN d.tags t ORDER BY t")
    List<String> findAllTags();

    // ===== STATISTICS METHODS =====
    
    // Count documents by category
    @Query("SELECT d.category as category, COUNT(d) as count FROM Document d " +
           "WHERE d.category IS NOT NULL " +
           "GROUP BY d.category " +
           "ORDER BY COUNT(d) DESC")
    List<Object[]> countByCategory();

    // Sum all download counts
    @Query("SELECT COALESCE(SUM(d.downloadCount), 0) FROM Document d")
    long sumDownloadCounts();

    // Get comprehensive document statistics
    @Query("SELECT " +
           "COUNT(d) as totalCount, " +
           "COUNT(CASE WHEN d.status = 'PENDING' THEN 1 END) as pendingCount, " +
           "COUNT(CASE WHEN d.status = 'APPROVED' THEN 1 END) as approvedCount, " +
           "COUNT(CASE WHEN d.status = 'REJECTED' THEN 1 END) as rejectedCount, " +
           "COALESCE(SUM(d.downloadCount), 0) as totalDownloads, " +
           "COALESCE(AVG(d.fileSize), 0) as avgFileSize " +
           "FROM Document d")
    Object[] getDocumentStatistics();

    // Get upload trends by date
    @Query("SELECT DATE(d.uploadDate) as uploadDate, COUNT(d) as count " +
           "FROM Document d " +
           "WHERE d.uploadDate >= :startDate " +
           "GROUP BY DATE(d.uploadDate) " +
           "ORDER BY DATE(d.uploadDate) DESC")
    List<Object[]> getUploadTrendsByDate(@Param("startDate") LocalDateTime startDate);

    // Find most downloaded documents
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.downloadCount > 0 ORDER BY d.downloadCount DESC")
    List<Document> findMostDownloadedDocuments(Pageable pageable);

    // Get file size statistics
    @Query("SELECT " +
           "COUNT(CASE WHEN d.fileSize < 1048576 THEN 1 END) as smallFiles, " +
           "COUNT(CASE WHEN d.fileSize BETWEEN 1048576 AND 10485760 THEN 1 END) as mediumFiles, " +
           "COUNT(CASE WHEN d.fileSize > 10485760 THEN 1 END) as largeFiles " +
           "FROM Document d")
    Object[] getFileSizeStatistics();
}
