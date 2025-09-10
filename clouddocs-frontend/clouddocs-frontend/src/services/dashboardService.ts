import api from './api';

export interface DashboardStats {
  totalDocuments: number;
  pendingDocuments: number;
  approvedDocuments: number;
  rejectedDocuments: number;
  totalUsers: number;
  recentUploads: number;
}

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

   // ✅ NEW: OCR fields (all optional)
  hasOcr?: boolean;
  ocrText?: string;
  ocrConfidence?: number;
  ocrProcessingTime?: number;
  
  // ✅ NEW: AI fields (all optional)
  embeddingGenerated?: boolean;
  aiScore?: number;
  searchType?: 'semantic' | 'keyword' | 'hybrid';
}

export interface DocumentUploadResponse {
  message: string;
  document: Document;
}

class DashboardService {
  
  // Get dashboard statistics
  async getDashboardStats(): Promise<DashboardStats> {
    try {
      const response = await api.get<DashboardStats>('/dashboard/stats');
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch dashboard stats');
    }
  }

  // Get recent documents
  async getRecentDocuments(limit: number = 10): Promise<Document[]> {
    try {
      const response = await api.get<Document[]>(`/dashboard/recent-documents?limit=${limit}`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch recent documents');
    }
  }

  // Upload document using your existing endpoint
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
        onUploadProgress: (progressEvent) => {
          if (progressEvent.total) {
            const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
            console.log(`Upload progress: ${progress}%`);
          }
        },
      });
      
      return response.data.document;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to upload document');
    }
  }

  // Get all documents with pagination and filtering
  async getAllDocuments(
    page: number = 0,
    size: number = 20,
    sortBy: string = 'uploadDate',
    sortDir: string = 'desc',
    search?: string,
    status?: string,
    category?: string
  ): Promise<{
    documents: Document[];
    currentPage: number;
    totalItems: number;
    totalPages: number;
    hasNext: boolean;
    hasPrevious: boolean;
  }> {
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

      const response = await api.get(`/documents?${params}`);
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to fetch documents');
    }
  }

  // Get pending documents for workflows
// Update getPendingWorkflows method
async getPendingWorkflows(limit: number = 10): Promise<Document[]> {
  try {
    const response = await api.get(`/documents/pending?page=0&size=${limit}`);
    return response.data.documents || [];
  } catch (error: any) {
    // If it's a permission error, return empty array instead of throwing
    if (error.response?.status === 403) {
      console.warn('No permission to access pending workflows');
      return []; // Return empty array for non-admin users
    }
    throw new Error(error.response?.data?.error || 'Failed to fetch pending workflows');
  }
}


  // Update document status
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

  // Download document
  async downloadDocument(id: number): Promise<Blob> {
    try {
      const response = await api.get(`/documents/${id}/download`, {
        responseType: 'blob'
      });
      return response.data;
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to download document');
    }
  }

  // Delete document
  async deleteDocument(id: number): Promise<void> {
    try {
      await api.delete(`/documents/${id}`);
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to delete document');
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

  // Bulk approve documents
  async bulkApproveDocuments(documentIds: number[]): Promise<{ successCount: number; errorCount: number }> {
    try {
      const response = await api.put('/documents/bulk-approve', documentIds);
      return {
        successCount: response.data.successCount,
        errorCount: response.data.errorCount
      };
    } catch (error: any) {
      throw new Error(error.response?.data?.error || 'Failed to bulk approve documents');
    }
  }
}

export default new DashboardService();
