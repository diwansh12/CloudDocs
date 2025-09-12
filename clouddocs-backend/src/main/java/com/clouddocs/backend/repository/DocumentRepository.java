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
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    // ===== BASIC FIND METHODS =====
    
    // Find documents by status
    Page<Document> findByStatus(DocumentStatus status, Pageable pageable);

    // Find documents by uploaded user
    Page<Document> findByUploadedBy(User user, Pageable pageable);

    // Find documents by uploaded user ID
    Page<Document> findByUploadedById(Long uploadedById, Pageable pageable);

    // Find documents by category
    Page<Document> findByCategory(String category, Pageable pageable);

    // Find documents by document type
    Page<Document> findByDocumentType(String documentType, Pageable pageable);

    // Find documents by status and user
    Page<Document> findByStatusAndUploadedBy(DocumentStatus status, User user, Pageable pageable);


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

    // ===== ✅ NEW: OPTIMIZED METHODS WITH JOIN FETCH TO PREVENT LAZY LOADING =====
    
    /**
     * ✅ SOLUTION: Fetch document by ID with tags and users to prevent LazyInitializationException
     */
   @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
       "WHERE d.id = :id AND d.deleted = false")
Optional<Document> findByIdWithTags(@Param("id") Long id);
    /**
     * ✅ SOLUTION: Fetch all documents with tags and users for better performance
     */
   @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE d.deleted = false")
Page<Document> findAllWithTagsAndUsers(Pageable pageable);


    /**
     * ✅ SOLUTION: Fetch user's documents with tags and users
     */
 @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
       "WHERE d.uploadedBy.id = :userId AND d.deleted = false ORDER BY d.uploadDate DESC")
Page<Document> findByUploadedByIdWithTagsAndUsers(@Param("userId") Long userId, Pageable pageable);

    /**
     * ✅ SOLUTION: Search with tags included
     */
   @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE " +
       "d.deleted = false AND (" +
       "LOWER(d.filename) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
       "LOWER(d.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
       "LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :query, '%')))")
Page<Document> searchDocumentsWithTags(@Param("query") String query, Pageable pageable);

    /**
     * ✅ SOLUTION: Find pending documents with all relationships
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.status = 'PENDING'")
    Page<Document> findPendingDocumentsWithTags(Pageable pageable);

    /**
     * ✅ SOLUTION: Find documents with embeddings AND tags for AI search
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND d.embeddingGenerated = true")
    List<Document> findByUploadedByUsernameAndEmbeddingGeneratedTrueWithTags(@Param("username") String username);

    // ===== EXISTING OPTIMIZED METHODS =====

    /**
     * ✅ CRITICAL: JOIN FETCH to prevent lazy loading exceptions
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.id = :userId ORDER BY d.uploadDate DESC")
    Page<Document> findByUploadedByIdOrderByUploadDateDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * ✅ CRITICAL: Search with JOIN FETCH to avoid lazy loading
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE " +
           "LOWER(d.filename) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Document> searchDocuments(@Param("query") String query, Pageable pageable);

    /**
     * ✅ CRITICAL: Find all with JOIN FETCH
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy")
    Page<Document> findAllWithUsers(Pageable pageable);

    /**
     * ✅ CRITICAL: Date range with JOIN FETCH
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadDate BETWEEN :startDate AND :endDate")
    Page<Document> findByUploadDateBetween(@Param("startDate") LocalDateTime startDate, 
                                         @Param("endDate") LocalDateTime endDate, 
                                         Pageable pageable);

    /**
     * ✅ CRITICAL: Pending documents with JOIN FETCH
     */
  @Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
       "WHERE d.status = 'PENDING' AND d.deleted = false")
Page<Document> findPendingDocuments(Pageable pageable);
    /**
     * ✅ CRITICAL: Recent documents with JOIN FETCH
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "ORDER BY d.uploadDate DESC")
    Page<Document> findRecentDocuments(Pageable pageable);

    /**
     * ✅ Advanced filter with JOIN FETCH to prevent lazy loading
     */
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
 @Query("SELECT COUNT(d) FROM Document d WHERE d.status = :status AND d.deleted = false")
long countByStatus(@Param("status") DocumentStatus status);
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
    
    /**
     * Find documents by tags with all relationships loaded
     */
    @Query("SELECT DISTINCT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "LEFT JOIN FETCH d.tags WHERE :tag MEMBER OF d.tags")
    Page<Document> findByTagsContaining(@Param("tag") String tag, Pageable pageable);

    /**
     * Find documents by multiple tags with relationships loaded
     */
    @Query("SELECT DISTINCT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "LEFT JOIN FETCH d.tags WHERE EXISTS (SELECT 1 FROM d.tags t WHERE t IN :tags)")
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

    // ===== ✅ AI SEARCH METHODS WITH JOIN FETCH OPTIMIZATION =====
    
    /**
     * Find documents with embeddings generated for a specific user (optimized)
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND d.embeddingGenerated = true")
    List<Document> findByUploadedByUsernameAndEmbeddingGeneratedTrue(@Param("username") String username);
    
    /**
     * Find documents without embeddings generated for a specific user (optimized)
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND (d.embeddingGenerated = false OR d.embeddingGenerated IS NULL)")
    List<Document> findByUploadedByUsernameAndEmbeddingGeneratedFalse(@Param("username") String username);

    /**
     * Count total documents by username
     */
   @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.username = :username AND d.deleted = false")
long countByUploadedByUsername(@Param("username") String username);

    /**
     * Count documents with embeddings generated for a specific user
     */
   @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.username = :username AND d.embeddingGenerated = true AND d.deleted = false")
long countByUploadedByUsernameAndEmbeddingGeneratedTrue(@Param("username") String username);


    /**
     * Count documents without embeddings generated for a specific user
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.username = :username AND (d.embeddingGenerated = false OR d.embeddingGenerated IS NULL)")
    long countByUploadedByUsernameAndEmbeddingGeneratedFalse(@Param("username") String username);

    /**
     * Find documents by username and filename containing (case insensitive) for keyword search fallback (optimized)
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :filename, '%'))")
    List<Document> findByUploadedByUsernameAndOriginalFilenameContainingIgnoreCase(@Param("username") String username, @Param("filename") String filename);

    /**
     * Advanced search by username and query across multiple fields for hybrid AI search (optimized)
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND (" +
           "LOWER(d.filename) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(COALESCE(d.description, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(COALESCE(d.category, '')) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(COALESCE(d.ocrText, '')) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Document> searchDocumentsByQuery(@Param("username") String username, @Param("query") String query);

    /**
     * Find documents by username (for general AI operations) (optimized)
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username ORDER BY d.uploadDate DESC")
    List<Document> findByUploadedByUsername(@Param("username") String username);

    /**
     * Find recent documents without embeddings for a user (for batch processing) (optimized)
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND (d.embeddingGenerated = false OR d.embeddingGenerated IS NULL) " +
           "ORDER BY d.uploadDate DESC")
    List<Document> findRecentDocumentsWithoutEmbeddings(@Param("username") String username, Pageable pageable);

    /**
     * Count documents by username and status for AI statistics
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.username = :username AND d.status = :status")
    long countByUploadedByUsernameAndStatus(@Param("username") String username, @Param("status") DocumentStatus status);

    // ===== ✅ OCR METHODS =====

    /**
     * Count documents with OCR processed for a specific user
     */
   @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.username = :username AND d.hasOcr = true AND d.deleted = false")
long countByUploadedByUsernameAndHasOcrTrue(@Param("username") String username);
    /**
     * Get average OCR confidence for user's documents
     */
    @Query("SELECT AVG(d.ocrConfidence) FROM Document d WHERE d.uploadedBy.username = :username AND d.ocrConfidence IS NOT NULL")
    Double getAverageOCRConfidenceByUser(@Param("username") String username);

    /**
     * Find documents with OCR processed for a user (optimized)
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND d.hasOcr = true ORDER BY d.uploadDate DESC")
    List<Document> findByUploadedByUsernameAndHasOcrTrue(@Param("username") String username);

    /**
     * Find documents without OCR for a user (optimized)
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND (d.hasOcr = false OR d.hasOcr IS NULL) " +
           "AND d.documentType = 'image' ORDER BY d.uploadDate DESC")
    List<Document> findImageDocumentsWithoutOCR(@Param("username") String username);

    /**
     * Search in OCR text content
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND d.hasOcr = true AND " +
           "LOWER(COALESCE(d.ocrText, '')) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Document> searchInOCRText(@Param("username") String username, @Param("query") String query);

    /**
     * Find documents with high OCR confidence
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND d.hasOcr = true AND d.ocrConfidence > :minConfidence " +
           "ORDER BY d.ocrConfidence DESC")
    List<Document> findHighConfidenceOCRDocuments(@Param("username") String username, @Param("minConfidence") Double minConfidence);

    // ===== ✅ ADVANCED AI & OCR COMBINED METHODS =====

    /**
     * Find documents that are both AI-ready and OCR-processed
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND d.embeddingGenerated = true AND d.hasOcr = true " +
           "ORDER BY d.uploadDate DESC")
    List<Document> findAIReadyDocumentsWithOCR(@Param("username") String username);

    /**
     * Count AI-ready documents with OCR
     */
    @Query("SELECT COUNT(d) FROM Document d WHERE d.uploadedBy.username = :username AND d.embeddingGenerated = true AND d.hasOcr = true")
    long countAIReadyDocumentsWithOCR(@Param("username") String username);

    /**
     * Find searchable documents (has embedding OR OCR text)
     */
    @Query("SELECT d FROM Document d LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy " +
           "WHERE d.uploadedBy.username = :username AND (d.embeddingGenerated = true OR d.hasOcr = true) " +
           "ORDER BY d.uploadDate DESC")
    List<Document> findSearchableDocuments(@Param("username") String username);

    /**
     * Get comprehensive AI and OCR statistics
     */
    @Query("SELECT " +
           "COUNT(d) as totalDocuments, " +
           "COUNT(CASE WHEN d.embeddingGenerated = true THEN 1 END) as documentsWithEmbeddings, " +
           "COUNT(CASE WHEN d.hasOcr = true THEN 1 END) as documentsWithOCR, " +
           "COUNT(CASE WHEN d.embeddingGenerated = true AND d.hasOcr = true THEN 1 END) as documentsWithBoth, " +
           "COUNT(CASE WHEN d.embeddingGenerated = true OR d.hasOcr = true THEN 1 END) as searchableDocuments, " +
           "AVG(CASE WHEN d.ocrConfidence IS NOT NULL THEN d.ocrConfidence END) as avgOCRConfidence " +
           "FROM Document d WHERE d.uploadedBy.username = :username")
    Object[] getAIAndOCRStatistics(@Param("username") String username);

    /**
 * ✅ Find deleted documents (for trash/recycle bin)
 */
@Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE d.deleted = true")
Page<Document> findDeletedDocuments(Pageable pageable);

/**
 * ✅ Find deleted document by ID
 */
@Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE d.id = :id AND d.deleted = true")
Optional<Document> findDeletedById(@Param("id") Long id);

/**
 * ✅ Find document by ID including deleted ones (for admin operations)
 */
@Query("SELECT d FROM Document d LEFT JOIN FETCH d.tags LEFT JOIN FETCH d.uploadedBy LEFT JOIN FETCH d.approvedBy WHERE d.id = :id")
Optional<Document> findByIdIncludingDeleted(@Param("id") Long id);


    
   
}
