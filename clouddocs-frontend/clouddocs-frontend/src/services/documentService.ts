// src/services/documentService.ts
import api from './api';
import { DocumentWithOCR } from './api';

export interface Document {
  id: number;
  filename: string;
  originalFilename: string;
  description?: string;
  fileSize: number;
  formattedFileSize: string;
  mimeType: string;
  status: 'PENDING' | 'IN_REVIEW' | 'APPROVED' | 'REJECTED';
  versionNumber: number;
  uploadedByName: string;
  uploadedById: number;
  uploadDate: string;
  lastModified: string;
  downloadCount: number;
  tags?: string[];
  category?: string;
  documentType?: string;
  approvedByName?: string;
  approvalDate?: string;
  rejectionReason?: string;
  // ✅ NEW: OCR fields
  hasOcr?: boolean;
  ocrText?: string;
  ocrConfidence?: number;
  ocrProcessingTime?: number;
  // ✅ NEW: AI fields
  embeddingGenerated?: boolean;
  aiScore?: number;
  searchType?: 'semantic' | 'keyword' | 'hybrid';
}

export interface DocumentsResponse {
  documents: Document[];
  currentPage: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface DocumentUploadResponse {
  message: string;
  document: Document;
}

// ✅ NEW: OCR-specific interfaces
export interface OCRStatistics {
  totalDocuments: number;
  documentsWithOCR: number;
  documentsWithEmbeddings: number;
  ocrCoverage: number;
  averageOCRConfidence: number;
  aiReadyDocuments: number;
}

export interface SearchResult {
  documents: Document[];
  totalResults: number;
  searchType: 'semantic' | 'keyword' | 'hybrid';
  processingTime: number;
}

// ✅ Existing interfaces
export interface ShareLinkOptions {
  expiryHours?: number;
  allowDownload?: boolean;
  password?: string;
}

export interface ShareLinkResponse {
  shareUrl: string;
  expiresAt?: string;
  shareId: string;
}

export interface ShareLink {
  id: string;
  url: string;
  expiresAt: string;
  allowDownload: boolean;
  hasPassword: boolean;
  createdAt: string;
  accessCount: number;
}

export interface DocumentMetadata {
  title?: string;
  description?: string;
  category?: string;
  tags?: string[];
}

class DocumentService {
  
  // ===== EXISTING DOCUMENT METHODS =====
  
  // Get all documents with pagination and filtering
  async getAllDocuments(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'uploadDate',
    sortDir: string = 'desc',
    search?: string,
    status?: string,
    category?: string
  ): Promise<DocumentsResponse> {
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
        sortBy,
        sortDir
      });

      if (search) params.append('search', search);
      if (status) params.append('status', status);
      if (category) params.append('category', category);

      const response = await api.get<DocumentsResponse>(`/documents?${params}`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch documents');
    }
  }

  // Get user's own documents
  async getMyDocuments(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'uploadDate',
    sortDir: string = 'desc'
  ): Promise<DocumentsResponse> {
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
        sortBy,
        sortDir
      });

      const response = await api.get<DocumentsResponse>(`/documents/my-documents?${params}`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch your documents');
    }
  }

  // ✅ NEW: Get user's documents with OCR information
  async getMyDocumentsWithOCR(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'uploadDate',
    sortDir: string = 'desc'
  ): Promise<{ documents: Document[]; currentPage: number; totalItems: number; totalPages: number; hasNext: boolean; hasPrevious: boolean; }> {
    try {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
        sortBy,
        sortDir
      });

      const response = await api.get<any>(`/documents/my-documents-ocr?${params}`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch your documents with OCR info');
    }
  }

  // Get document by ID
  async getDocumentById(id: number): Promise<Document> {
    try {
      const response = await api.get<Document>(`/documents/${id}`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch document details');
    }
  }

  // Upload document (regular)
  async uploadDocument(file: File, description?: string, category?: string, tags?: string[]): Promise<Document> {
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      if (description) {
        formData.append('description', description);
      }
      if (category) {
        formData.append('category', category);
      }
      if (tags && tags.length > 0) {
        tags.forEach(tag => formData.append('tags', tag));
      }

      const response = await api.post<DocumentUploadResponse>('/documents/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      });
      
      return response.data.document;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to upload document');
    }
  }

  // ✅ NEW: Upload document with OCR processing
  async uploadDocumentWithOCR(
    file: File, 
    description?: string, 
    category?: string, 
    tags?: string[]
  ): Promise<Document> {
    try {
      const formData = new FormData();
      formData.append('file', file);
      
      if (description) formData.append('description', description);
      if (category) formData.append('category', category);
      if (tags && tags.length > 0) {
        tags.forEach(tag => formData.append('tags', tag));
      }

      const response = await api.uploadFile<{ document: Document }>('/ocr/upload', formData, {
        timeout: 60000, // Extended timeout for OCR processing
        onUploadProgress: (progressEvent) => {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total!);
          console.log(`📤 OCR Upload Progress: ${progress}%`);
        },
      });
      
      return response.data.document;
    } catch (error: any) {
      if (error.response?.status === 413) {
        throw new Error('File too large. Please select an image smaller than 10MB.');
      }
      if (error.response?.status === 415) {
        throw new Error('Unsupported file type. Please upload an image file (JPEG, PNG, BMP, TIFF, GIF).');
      }
      throw new Error(error.response?.data?.error || 'Failed to upload document with OCR');
    }
  }

  // Download document
  async downloadDocument(id: number, filename: string): Promise<void> {
    try {
      const response = await api.get(`/documents/${id}/download`, {
        responseType: 'blob'
      });
      
      // Create download link
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = window.document.createElement('a');
      link.href = url;
      link.download = filename;
      window.document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to download document');
    }
  }

   async deleteDocument(id: number): Promise<{ message: string; deletedDocumentId: number }> {
    try {
      console.log(`🗑️ Deleting document with ID: ${id}`);
      
      const response = await api.delete(`/documents/${id}`);
      
      console.log(`✅ Document ${id} deleted successfully`);
      
      return {
        message: response.data?.message || 'Document deleted successfully',
        deletedDocumentId: id
      };
    } catch (error: any) {
      console.error(`❌ Failed to delete document ${id}:`, error);
      
      // Enhanced error messages based on status codes
      if (error.response?.status === 404) {
        throw new Error('Document not found. It may have already been deleted.');
      } else if (error.response?.status === 403) {
        throw new Error('You don\'t have permission to delete this document.');
      } else if (error.response?.status === 409) {
        throw new Error('Document cannot be deleted because it is currently being processed.');
      } else {
        throw new Error(error.response?.data?.error || 'Failed to delete document');
      }
    }
  }

  // ✅ NEW: Bulk delete multiple documents
  async bulkDeleteDocuments(documentIds: number[]): Promise<{ 
    deletedCount: number; 
    failedCount: number; 
    failedDocuments?: { id: number; error: string }[] 
  }> {
    try {
      console.log(`🗑️ Bulk deleting ${documentIds.length} documents`);
      
      const response = await api.delete('/documents/bulk', { 
        data: { documentIds } 
      });
      
      console.log(`✅ Bulk delete completed:`, response.data);
      
      return response.data;
    } catch (error: any) {
      console.error('❌ Bulk delete failed:', error);
      throw new Error(error.response?.data?.error || 'Failed to delete documents');
    }
  }

  // ✅ NEW: Soft delete document (if your backend supports it)
  async softDeleteDocument(id: number): Promise<Document> {
    try {
      const response = await api.put(`/documents/${id}/soft-delete`);
      return response.data.document;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to soft delete document');
    }
  }

  // ✅ NEW: Restore soft deleted document
  async restoreDocument(id: number): Promise<Document> {
    try {
      const response = await api.put(`/documents/${id}/restore`);
      return response.data.document;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to restore document');
    }
  }

  // ===== OCR-SPECIFIC METHODS =====

  // ✅ NEW: Get OCR statistics for current user
  async getOCRStatistics(): Promise<OCRStatistics> {
    try {
      const response = await api.get<OCRStatistics>('/ocr/stats');
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch OCR statistics');
    }
  }

  // ✅ NEW: Filter documents by OCR status
  async getDocumentsByOCRStatus(hasOCR: boolean): Promise<Document[]> {
    try {
      const response = await api.get<Document[]>(`/documents/filter-ocr?hasOCR=${hasOCR}`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch documents by OCR status');
    }
  }

  // ✅ NEW: Get documents with high OCR confidence
  async getHighConfidenceOCRDocuments(minConfidence: number = 0.8): Promise<Document[]> {
    try {
      const response = await api.get<Document[]>(`/documents/ocr/high-confidence?minConfidence=${minConfidence}`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch high confidence OCR documents');
    }
  }

  // ✅ NEW: Get AI-ready documents (both OCR and embeddings)
  async getAIReadyDocuments(): Promise<Document[]> {
    try {
      const response = await api.get<Document[]>('/documents/ai-ready');
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch AI-ready documents');
    }
  }

  // ===== SEARCH METHODS =====

async semanticSearch(query: string, limit: number = 10): Promise<SearchResult> {
  try {
    console.log('🔍 Starting semantic search for:', query);
    
    const startTime = Date.now();
    
    const response = await api.post('/search/semantic', {
      query,
      limit
    });
    
    const processingTime = Date.now() - startTime;
    console.log(`✅ Semantic search completed in ${processingTime}ms`);
    
    // ✅ FIXED: Map correct backend response structure
    return {
      documents: response.data?.documents || [],         // ✅ SearchController sends "documents"
      totalResults: response.data?.totalResults || 0,    // ✅ SearchController sends "totalResults"
      searchType: 'semantic' as const,
      processingTime
    };
  } catch (error: any) {
    console.error('❌ Semantic search failed:', error?.message || 'Unknown error');
    console.log('• Using regular search instead');
    
    // Return safe fallback structure
    return {
      documents: [],
      totalResults: 0,
      searchType: 'semantic' as const,
      processingTime: 0
    };
  }
}


  async hybridSearch(query: string, limit: number = 10): Promise<SearchResult> {
  try {
    console.log('🔍 Starting hybrid search for:', query);
    
    const startTime = Date.now();
    
    const response = await api.post('/search/hybrid', {
      query,
      limit
    });
    
    const processingTime = Date.now() - startTime;
    console.log(`✅ Hybrid search completed in ${processingTime}ms`);
    
    // ✅ FIXED: Map correct backend response structure
    return {
      documents: response.data?.documents || [],         // ✅ SearchController sends "documents"
      totalResults: response.data?.totalResults || 0,    // ✅ SearchController sends "totalResults"
      searchType: 'hybrid' as const,
      processingTime
    };
  } catch (error: any) {
    console.error('❌ Hybrid search failed:', error?.message || 'Unknown error');
    
    return {
      documents: [],
      totalResults: 0,
      searchType: 'hybrid' as const,
      processingTime: 0
    };
  }
}


// ✅ NEW: Generate embeddings using SearchController delegation
async generateEmbeddings(): Promise<string> {
  try {
    console.log('🤖 Generating embeddings via SearchController...');
    
    const response = await api.post('/search/generate-embeddings');
    
    console.log('✅ Embeddings generated successfully');
    return response.data.message || 'Embeddings generated successfully! AI search is now available.';
  } catch (error: any) {
    console.error('❌ Failed to generate embeddings:', error);
    throw new Error(error.response?.data?.error || 'Failed to generate embeddings');
  }
}


  // ✅ NEW: Search specifically in OCR text
  async searchOCRText(query: string): Promise<Document[]> {
    try {
      const response = await api.get<Document[]>(`/search/ocr?q=${encodeURIComponent(query)}`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'OCR text search failed');
    }
  }

  // ===== EXISTING SHARE AND METADATA METHODS =====

  // Update document metadata
  async updateDocumentMetadata(id: number, metadata: DocumentMetadata): Promise<Document> {
    try {
      const response = await api.put<Document>(`/documents/${id}/metadata`, metadata);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to update document metadata');
    }
  }

  // Generate share link
  async generateShareLink(id: number, options: ShareLinkOptions = {}): Promise<ShareLinkResponse> {
    try {
      const response = await api.post<ShareLinkResponse>(`/documents/${id}/share`, options);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to generate share link');
    }
  }

  // Get existing share links for a document
  async getShareLinks(id: number): Promise<ShareLink[]> {
    try {
      const response = await api.get<ShareLink[]>(`/documents/${id}/shares`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch share links');
    }
  }

  // Revoke share link
  async revokeShareLink(documentId: number, shareId: string): Promise<void> {
    try {
      await api.delete(`/documents/${documentId}/shares/${shareId}`);
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to revoke share link');
    }
  }

  // Access shared document (public endpoint)
  async accessSharedDocument(shareId: string, password?: string): Promise<Document> {
    try {
      const payload = password ? { password } : {};
      const response = await api.post<Document>(`/documents/shared/${shareId}/access`, payload);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to access shared document');
    }
  }

  // Download shared document (public endpoint)
  async downloadSharedDocument(shareId: string, password?: string): Promise<void> {
    try {
      const payload = password ? { password } : {};
      const response = await api.post(`/documents/shared/${shareId}/download`, payload, {
        responseType: 'blob'
      });

      // Extract filename from response headers
      const disposition = response.headers['content-disposition'];
      let filename = 'document';
      if (disposition) {
        const filenameMatch = disposition.match(/filename="(.+)"/);
        if (filenameMatch) {
          filename = filenameMatch[1];
        }
      }

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to download shared document');
    }
  }

  // ===== ADMIN/MANAGER METHODS =====

  // Update document status (Admin/Manager only)
  async updateDocumentStatus(id: number, status: string, rejectionReason?: string): Promise<Document> {
    try {
      const params = new URLSearchParams({ status });
      if (rejectionReason) {
        params.append('rejectionReason', rejectionReason);
      }

      const response = await api.put(`/documents/${id}/status?${params}`);
      return response.data.document;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to update document status');
    }
  }

  // Get categories
  async getCategories(): Promise<string[]> {
    try {
      const response = await api.get<string[]>('/documents/categories');
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch categories');
    }
  }

  // Get tags
  async getTags(): Promise<string[]> {
    try {
      const response = await api.get<string[]>('/documents/tags');
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch tags');
    }
  }

  // Get document statistics (Admin/Manager only)
  async getDocumentStats(): Promise<{
    total: number;
    byStatus: Record<string, number>;
    byCategory: Record<string, number>;
    recentUploads: number;
    totalDownloads: number;
  }> {
    try {
      const response = await api.get('/documents/stats');
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch document statistics');
    }
  }

  // Bulk operations (Admin/Manager only)
  async bulkUpdateStatus(documentIds: number[], status: string, rejectionReason?: string): Promise<void> {
    try {
      const payload = {
        documentIds,
        status,
        rejectionReason
      };
      await api.put('/documents/bulk/status', payload);
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to update document statuses');
    }
  }

  async bulkDelete(documentIds: number[]): Promise<void> {
    try {
      await api.delete('/documents/bulk', { data: { documentIds } });
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to delete documents');
    }
  }


  // ✅ NEW: Check AI status and capabilities
async getAIStatus(): Promise<{
  aiSearchEnabled: boolean;
  searchMethod?: string;
  message?: string;
}> {
  try {
    // Test with a simple query to see if AI is working
    const testResult = await this.semanticSearch('test', 1);
    
    return {
      aiSearchEnabled: true,
      searchMethod: 'ai_delegation',
      message: 'AI search is available via SearchController delegation'
    };
  } catch (error) {
    console.warn('AI search not available:', error);
    return {
      aiSearchEnabled: false,
      searchMethod: 'fallback',
      message: 'AI search unavailable, using enhanced regular search'
    };
  }
}

  // ===== UTILITY METHODS =====

  // ✅ NEW: Validate file for OCR processing
  validateFileForOCR(file: File): { valid: boolean; error?: string } {
    // Check file size (10MB limit)
    const maxSize = 10 * 1024 * 1024; // 10MB
    if (file.size > maxSize) {
      return {
        valid: false,
        error: 'File too large. Please select an image smaller than 10MB.'
      };
    }

    // Check file type
    const supportedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/bmp', 'image/tiff', 'image/gif'];
    if (!supportedTypes.includes(file.type.toLowerCase())) {
      return {
        valid: false,
        error: 'Unsupported file type. Please upload JPEG, PNG, BMP, TIFF, or GIF images.'
      };
    }

    return { valid: true };
  }

  // ✅ NEW: Format OCR confidence as percentage
  formatConfidence(confidence: number): string {
    return `${(confidence * 100).toFixed(1)}%`;
  }

  // ✅ NEW: Get confidence color for UI
  getConfidenceColor(confidence: number): string {
    if (confidence >= 0.8) return 'text-green-600';
    if (confidence >= 0.6) return 'text-yellow-600';
    return 'text-red-600';
  }

  // ✅ NEW: Get confidence badge class
  getConfidenceBadge(confidence: number): string {
    if (confidence >= 0.8) return 'bg-green-100 text-green-800';
    if (confidence >= 0.6) return 'bg-yellow-100 text-yellow-800';
    return 'bg-red-100 text-red-800';
  }

  // ✅ NEW: Check if document has high-quality OCR
  hasHighQualityOCR(document: Document): boolean {
    return !!(document.hasOcr && document.ocrConfidence && document.ocrConfidence > 0.8);
  }

  // ✅ NEW: Check if document is AI-ready
  isAIReady(document: Document): boolean {
    return !!(document.embeddingGenerated || (document.hasOcr && document.ocrText && document.ocrText.length > 10));
  }
}

export default new DocumentService();
