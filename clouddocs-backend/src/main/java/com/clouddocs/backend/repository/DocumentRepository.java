package com.clouddocs.backend.repository;

import com.clouddocs.backend.entity.Document;
import com.clouddocs.backend.entity.DocumentStatus;
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

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {



        /**
     * ✅ FIX: Increment download count for a document
     */
    @Modifying
    @Transactional
    @Query("UPDATE Document d SET d.downloadCount = d.downloadCount + 1 WHERE d.id = :id")
    void incrementDownloadCount(@Param("id") Long id);
    
    /**
     * ✅ ADDITIONAL: Decrement download count (if needed)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Document d SET d.downloadCount = d.downloadCount - 1 WHERE d.id = :id AND d.downloadCount > 0")
    void decrementDownloadCount(@Param("id") Long id);
    
    /**
     * ✅ ADDITIONAL: Reset download count
     */
    @Modifying
    @Transactional
    @Query("UPDATE Document d SET d.downloadCount = 0 WHERE d.id = :id")
    void resetDownloadCount(@Param("id") Long id);
    
    /**
     * ✅ ADDITIONAL: Batch increment download count
     */
    @Modifying
    @Transactional
    @Query("UPDATE Document d SET d.downloadCount = d.downloadCount + 1 WHERE d.id IN :ids")
    void incrementDownloadCountBatch(@Param("ids") List<Long> ids);

    // ===== ✅ SOFT DELETE COUNT METHODS (REQUIRED FOR DASHBOARD) =====
    
    /**
     * Count all non-deleted documents
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.deleted = false")
    long countByDeletedFalse();
    
    /**
     * Count documents by status excluding deleted
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.status = :status AND d.deleted = false")
    long countByStatusAndDeletedFalse(@Param("status") DocumentStatus status);
    
    /**
     * Count user documents excluding deleted
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.id = :userId AND d.deleted = false")
    long countByUploadedByIdAndDeletedFalse(@Param("userId") Long userId);
    
    /**
     * Count user documents by status excluding deleted
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.id = :userId AND d.status = :status AND d.deleted = false")
    long countByUploadedByIdAndStatusAndDeletedFalse(@Param("userId") Long userId, @Param("status") DocumentStatus status);
    
    /**
     * Count recent uploads excluding deleted
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadDate > :date AND d.deleted = false")
    long countByUploadDateAfterAndDeletedFalse(@Param("date") LocalDateTime date);

    // ===== ✅ SOFT DELETE FETCH METHODS (REQUIRED FOR DASHBOARD) =====
    
    /**
     * Get recent documents excluding soft-deleted ones (for admins)
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.deleted = false ORDER BY d.uploadDate DESC")
    Page<Document> findRecentDocumentsExcludingDeleted(Pageable pageable);
    
    /**
     * Get user's recent documents excluding soft-deleted ones
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.id = :userId AND d.deleted = false ORDER BY d.uploadDate DESC")
    Page<Document> findByUploadedByIdOrderByUploadDateDescExcludingDeleted(@Param("userId") Long userId, Pageable pageable);

    // ===== ✅ SOFT DELETE MANAGEMENT METHODS =====
    
    /**
     * Find deleted documents (for trash/recycle bin)
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE d.deleted = true")
    Page<Document> findDeletedDocuments(Pageable pageable);
    
    /**
     * Find deleted document by ID
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE d.id = :id AND d.deleted = true")
    Optional<Document> findDeletedById(@Param("id") Long id);
    
    /**
     * Find document by ID including deleted ones (for admin operations)
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE d.id = :id")
    Optional<Document> findByIdIncludingDeleted(@Param("id") Long id);

    // ===== BASIC FIND METHODS (UPDATED TO EXCLUDE DELETED) =====
    
    /**
     * Find documents by status excluding deleted
     */
    @Query("SELECT d FROM Document d WHERE d.status = :status AND d.deleted = false")
    Page<Document> findByStatus(@Param("status") DocumentStatus status, Pageable pageable);

    /**
     * Find documents by uploaded user excluding deleted
     */
    @Query("SELECT d FROM Document d WHERE d.uploadedBy = :user AND d.deleted = false")
    Page<Document> findByUploadedBy(@Param("user") User user, Pageable pageable);

    /**
     * Find documents by uploaded user ID excluding deleted
     */
    @Query("SELECT d FROM Document d WHERE d.uploadedBy.id = :uploadedById AND d.deleted = false")
    Page<Document> findByUploadedById(@Param("uploadedById") Long uploadedById, Pageable pageable);

    /**
     * Find documents by category excluding deleted
     */
    @Query("SELECT d FROM Document d WHERE d.category = :category AND d.deleted = false")
    Page<Document> findByCategory(@Param("category") String category, Pageable pageable);

    /**
     * Find documents by document type excluding deleted
     */
    @Query("SELECT d FROM Document d WHERE d.documentType = :documentType AND d.deleted = false")
    Page<Document> findByDocumentType(@Param("documentType") String documentType, Pageable pageable);

    /**
     * Find documents by status and user excluding deleted
     */
    @Query("SELECT d FROM Document d WHERE d.status = :status AND d.uploadedBy = :user AND d.deleted = false")
    Page<Document> findByStatusAndUploadedBy(@Param("status") DocumentStatus status, @Param("user") User user, Pageable pageable);

    // ===== ✅ AI/OCR METHODS (UPDATED TO EXCLUDE DELETED) =====

    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.deleted = false AND d.embeddingGenerated = true")
    List<Document> findByEmbeddingGeneratedTrue();
    
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.deleted = false AND d.hasOcr = true")
    List<Document> findByHasOcrTrue();
    
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.deleted = false AND (d.hasOcr = false OR d.hasOcr IS NULL)")
    List<Document> findByHasOcrFalseOrHasOcrIsNull();
    
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.deleted = false AND d.hasOcr = true")
    Page<Document> findByHasOcrTrue(Pageable pageable);

    // ===== ✅ OPTIMIZED METHODS WITH JOIN FETCH (UPDATED TO EXCLUDE DELETED) =====
    
    /**
     * Fetch document by ID with tags and users excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.id = :id AND d.deleted = false")
    Optional<Document> findByIdWithTags(@Param("id") Long id);
    
    /**
     * Fetch all documents with tags and users excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE d.deleted = false")
    Page<Document> findAllWithTagsAndUsers(Pageable pageable);

    /**
     * Fetch user's documents with tags and users excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.id = :userId AND d.deleted = false ORDER BY d.uploadDate DESC")
    Page<Document> findByUploadedByIdWithTagsAndUsers(@Param("userId") Long userId, Pageable pageable);

    /**
     * Search with tags included excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE " +
           "d.deleted = false AND (" +
           "LOWER(d.filename) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Document> searchDocumentsWithTags(@Param("query") String query, Pageable pageable);

    /**
     * Find pending documents with all relationships excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.status = 'PENDING' AND d.deleted = false")
    Page<Document> findPendingDocumentsWithTags(Pageable pageable);

    /**
     * Find documents with embeddings AND tags for AI search excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND d.embeddingGenerated = true AND d.deleted = false")
    List<Document> findByUploadedByUsernameAndEmbeddingGeneratedTrueWithTags(@Param("username") String username);

    // ===== EXISTING OPTIMIZED METHODS (UPDATED TO EXCLUDE DELETED) =====

    /**
     * JOIN FETCH to prevent lazy loading exceptions excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.id = :userId AND d.deleted = false ORDER BY d.uploadDate DESC")
    Page<Document> findByUploadedByIdOrderByUploadDateDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * Search with JOIN FETCH to avoid lazy loading excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE " +
           "d.deleted = false AND (" +
           "LOWER(d.filename) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Document> searchDocuments(@Param("query") String query, Pageable pageable);

    /**
     * Find all with JOIN FETCH excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE d.deleted = false")
    Page<Document> findAllWithUsers(Pageable pageable);

    /**
     * Date range with JOIN FETCH excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadDate BETWEEN :startDate AND :endDate AND d.deleted = false")
    Page<Document> findByUploadDateBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate, 
                                         Pageable pageable);

    /**
     * Pending documents with JOIN FETCH excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.status = 'PENDING' AND d.deleted = false")
    Page<Document> findPendingDocuments(Pageable pageable);
    
    /**
     * Recent documents with JOIN FETCH excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.deleted = false ORDER BY d.uploadDate DESC")
    Page<Document> findRecentDocuments(Pageable pageable);

    /**
     * Advanced filter with JOIN FETCH excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE " +
           "d.deleted = false AND " +
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

    // ===== COUNT METHODS (UPDATED TO EXCLUDE DELETED) =====
    
    /**
     * Count documents by status excluding deleted
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.status = :status AND d.deleted = false")
    long countByStatus(@Param("status") DocumentStatus status);
    
    /**
     * Count documents by user excluding deleted
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy = :user AND d.deleted = false")
    long countByUploadedBy(@Param("user") User user);

    /**
     * Count documents by user ID excluding deleted
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.id = :uploadedById AND d.deleted = false")
    long countByUploadedById(@Param("uploadedById") Long uploadedById);

    /**
     * Count documents by user ID and status excluding deleted
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.id = :uploadedById AND d.status = :status AND d.deleted = false")
    long countByUploadedByIdAndStatus(@Param("uploadedById") Long uploadedById, @Param("status") DocumentStatus status);

    /**
     * Count documents uploaded after specific date excluding deleted
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadDate > :date AND d.deleted = false")
    long countByUploadDateAfter(@Param("date") LocalDateTime date);

    // ===== TAGS AND CATEGORIES (UPDATED TO EXCLUDE DELETED) =====
    
    /**
     * Find documents by tags with all relationships loaded excluding deleted
     */
    @Query("SELECT DISTINCT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "LEFT JOIN FETCH d.tags WHERE :tag MEMBER OF d.tags AND d.deleted = false")
    Page<Document> findByTagsContaining(@Param("tag") String tag, Pageable pageable);

    /**
     * Find documents by multiple tags with relationships loaded excluding deleted
     */
    @Query("SELECT DISTINCT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "LEFT JOIN FETCH d.tags WHERE EXISTS (SELECT 1 FROM d.tags t WHERE t IN :tags) AND d.deleted = false")
    Page<Document> findByTagsIn(@Param("tags") List<String> tags, Pageable pageable);

    /**
     * Get all unique categories from non-deleted documents
     */
    @Query("SELECT DISTINCT d.category FROM Document d WHERE d.category IS NOT NULL AND d.deleted = false ORDER BY d.category")
    List<String> findAllCategories();

    /**
     * Get all unique tags from non-deleted documents
     */
    @Query("SELECT DISTINCT t FROM Document d JOIN d.tags t WHERE d.deleted = false ORDER BY t")
    List<String> findAllTags();

    // ===== STATISTICS METHODS (UPDATED TO EXCLUDE DELETED) =====
    
    /**
     * Count documents by category excluding deleted
     */
    @Query("SELECT d.category as category, COUNT(d) as count FROM Document d " +
           "WHERE d.category IS NOT NULL AND d.deleted = false " +
           "GROUP BY d.category " +
           "ORDER BY COUNT(d) DESC")
    List<Object[]> countByCategory();

    /**
     * Sum all download counts from non-deleted documents
     */
    @Query("SELECT COALESCE(SUM(d.downloadCount), 0) FROM Document d WHERE d.deleted = false")
    long sumDownloadCounts();

    /**
     * Get comprehensive document statistics excluding deleted
     */
    @Query("SELECT " +
           "COUNT(d) as totalCount, " +
           "COUNT(CASE WHEN d.status = 'PENDING' THEN 1 END) as pendingCount, " +
           "COUNT(CASE WHEN d.status = 'APPROVED' THEN 1 END) as approvedCount, " +
           "COUNT(CASE WHEN d.status = 'REJECTED' THEN 1 END) as rejectedCount, " +
           "COALESCE(SUM(d.downloadCount), 0) as totalDownloads, " +
           "COALESCE(AVG(d.fileSize), 0) as avgFileSize " +
           "FROM Document d WHERE d.deleted = false")
    Object[] getDocumentStatistics();

    /**
     * Get upload trends by date excluding deleted
     */
    @Query("SELECT DATE(d.uploadDate) as uploadDate, COUNT(d) as count " +
           "FROM Document d " +
           "WHERE d.uploadDate >= :startDate AND d.deleted = false " +
           "GROUP BY DATE(d.uploadDate) " +
           "ORDER BY DATE(d.uploadDate) DESC")
    List<Object[]> getUploadTrendsByDate(@Param("startDate") LocalDateTime startDate);

    /**
     * Find most downloaded documents excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.downloadCount > 0 AND d.deleted = false ORDER BY d.downloadCount DESC")
    List<Document> findMostDownloadedDocuments(Pageable pageable);

    /**
     * Get file size statistics excluding deleted
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN d.fileSize < 1048576 THEN 1 END) as smallFiles, " +
           "COUNT(CASE WHEN d.fileSize BETWEEN 1048576 AND 10485760 THEN 1 END) as mediumFiles, " +
           "COUNT(CASE WHEN d.fileSize > 10485760 THEN 1 END) as largeFiles " +
           "FROM Document d WHERE d.deleted = false")
    Object[] getFileSizeStatistics();

    // ===== ✅ AI SEARCH METHODS (UPDATED TO EXCLUDE DELETED) =====
    
    /**
     * Find documents with embeddings generated for a specific user excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND d.embeddingGenerated = true AND d.deleted = false")
    List<Document> findByUploadedByUsernameAndEmbeddingGeneratedTrue(@Param("username") String username);
    
    /**
     * Find documents without embeddings generated for a specific user excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND (d.embeddingGenerated = false OR d.embeddingGenerated IS NULL) AND d.deleted = false")
    List<Document> findByUploadedByUsernameAndEmbeddingGeneratedFalse(@Param("username") String username);

    /**
     * Count total documents by username excluding deleted
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.username = :username AND d.deleted = false")
    long countByUploadedByUsername(@Param("username") String username);

    /**
     * Count documents with embeddings generated for a specific user excluding deleted
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.username = :username AND d.embeddingGenerated = true AND d.deleted = false")
    long countByUploadedByUsernameAndEmbeddingGeneratedTrue(@Param("username") String username);

    /**
     * Count documents without embeddings generated for a specific user excluding deleted
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.username = :username AND (d.embeddingGenerated = false OR d.embeddingGenerated IS NULL) AND d.deleted = false")
    long countByUploadedByUsernameAndEmbeddingGeneratedFalse(@Param("username") String username);

    /**
     * Find documents by username and filename containing excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :filename, '%')) AND d.deleted = false")
    List<Document> findByUploadedByUsernameAndOriginalFilenameContainingIgnoreCase(@Param("username") String username, @Param("filename") String filename);

    /**
     * Advanced search by username and query across multiple fields excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND d.deleted = false AND (" +
           "LOWER(d.filename) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(COALESCE(d.category, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(COALESCE(d.ocrText, '')) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Document> searchDocumentsByQuery(@Param("username") String username, @Param("query") String query);

    /**
     * Find documents by username excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND d.deleted = false ORDER BY d.uploadDate DESC")
    List<Document> findByUploadedByUsername(@Param("username") String username);

    /**
     * Find recent documents without embeddings for a user excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND (d.embeddingGenerated = false OR d.embeddingGenerated IS NULL) " +
           "AND d.deleted = false ORDER BY d.uploadDate DESC")
    List<Document> findRecentDocumentsWithoutEmbeddings(@Param("username") String username, Pageable pageable);

    /**
     * Count documents by username and status excluding deleted
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.username = :username AND d.status = :status AND d.deleted = false")
    long countByUploadedByUsernameAndStatus(@Param("username") String username, @Param("status") DocumentStatus status);

    // ===== ✅ OCR METHODS (UPDATED TO EXCLUDE DELETED) =====

    /**
     * Count documents with OCR processed for a specific user excluding deleted
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.username = :username AND d.hasOcr = true AND d.deleted = false")
    long countByUploadedByUsernameAndHasOcrTrue(@Param("username") String username);
    
    /**
     * Get average OCR confidence for user's documents excluding deleted
     */
    @Query("SELECT AVG(d.ocrConfidence) FROM Document d WHERE d.uploadedBy.username = :username AND d.ocrConfidence IS NOT NULL AND d.deleted = false")
    Double getAverageOCRConfidenceByUser(@Param("username") String username);

    /**
     * Find documents with OCR processed for a user excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND d.hasOcr = true AND d.deleted = false ORDER BY d.uploadDate DESC")
    List<Document> findByUploadedByUsernameAndHasOcrTrue(@Param("username") String username);

    /**
     * Find documents without OCR for a user excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND (d.hasOcr = false OR d.hasOcr IS NULL) " +
           "AND d.documentType = 'image' AND d.deleted = false ORDER BY d.uploadDate DESC")
    List<Document> findImageDocumentsWithoutOCR(@Param("username") String username);

    /**
     * Search in OCR text content excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND d.hasOcr = true AND d.deleted = false AND " +
           "LOWER(COALESCE(d.ocrText, '')) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Document> searchInOCRText(@Param("username") String username, @Param("query") String query);

    /**
     * Find documents with high OCR confidence excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND d.hasOcr = true AND d.ocrConfidence > :minConfidence " +
           "AND d.deleted = false ORDER BY d.ocrConfidence DESC")
    List<Document> findHighConfidenceOCRDocuments(@Param("username") String username, @Param("minConfidence") Double minConfidence);

    // ===== ✅ ADVANCED AI & OCR COMBINED METHODS (UPDATED TO EXCLUDE DELETED) =====

    /**
     * Find documents that are both AI-ready and OCR-processed excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND d.embeddingGenerated = true AND d.hasOcr = true " +
           "AND d.deleted = false ORDER BY d.uploadDate DESC")
    List<Document> findAIReadyDocumentsWithOCR(@Param("username") String username);

    /**
     * Count AI-ready documents with OCR excluding deleted
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.username = :username AND d.embeddingGenerated = true AND d.hasOcr = true AND d.deleted = false")
    long countAIReadyDocumentsWithOCR(@Param("username") String username);

    /**
     * Find searchable documents (has embedding OR OCR text) excluding deleted
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND (d.embeddingGenerated = true OR d.hasOcr = true) " +
           "AND d.deleted = false ORDER BY d.uploadDate DESC")
    List<Document> findSearchableDocuments(@Param("username") String username);

    /**
     * Get comprehensive AI and OCR statistics excluding deleted
     */
    @Query("SELECT " +
           "COUNT(d) as totalDocuments, " +
           "COUNT(CASE WHEN d.embeddingGenerated = true THEN 1 END) as documentsWithEmbeddings, " +
           "COUNT(CASE WHEN d.hasOcr = true THEN 1 END) as documentsWithOCR, " +
           "COUNT(CASE WHEN d.embeddingGenerated = true AND d.hasOcr = true THEN 1 END) as documentsWithBoth, " +
           "COUNT(CASE WHEN d.embeddingGenerated = true OR d.hasOcr = true THEN 1 END) as searchableDocuments, " +
           "AVG(CASE WHEN d.ocrConfidence IS NOT NULL THEN d.ocrConfidence END) as avgOCRConfidence " +
           "FROM Document d WHERE d.uploadedBy.username = :username AND d.deleted = false")
    Object[] getAIAndOCRStatistics(@Param("username") String username);
}
